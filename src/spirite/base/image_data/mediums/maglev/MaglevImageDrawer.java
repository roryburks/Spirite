package spirite.base.image_data.mediums.maglev;

import spirite.base.brains.tools.ToolSchemes;
import spirite.base.brains.tools.ToolSchemes.MagneticFillMode;
import spirite.base.brains.tools.ToolSchemes.PenDrawMode;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.mediums.BuiltMediumData;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.*;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill.StrokeSegment;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.image_data.selection.ALiftedData;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.DrawPoints;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeParams;
import spirite.base.util.MUtil;
import spirite.base.util.interpolation.Interpolator2D;
import spirite.base.util.linear.MatTrans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MaglevImageDrawer 
	implements 	IImageDrawer,
				IStrokeModule,
				ITransformModule,
				IMagneticFillModule,
				IWeightEraserModule,
				IBoneDrawer,
				IColorChangeModule,
				ILiftSelectionModule,
				IAnchorLiftModule
{

	private final BuildingMediumData building;
	private final MaglevMedium img;
	
	private final MLDModuleWeightEraser eraserModule;
	private final MLDModuleLift liftModule;
	
	public MaglevImageDrawer( MaglevMedium img, BuildingMediumData building) {
		this.img = img;
		this.building = building;
		
		eraserModule = new MLDModuleWeightEraser(img, building);
		liftModule = new MLDModuleLift(img, building);
	}

	
	// ::: IStrokeModule
	StrokeEngine strokeEngine = null;
	@Override public StrokeEngine getStrokeEngine() {return strokeEngine;}

	@Override
	public boolean canDoStroke(StrokeEngine.Method method) {
		return true;
	}

	@Override
	public boolean startStroke(StrokeParams params, PenState ps) {
		ImageWorkspace workspace = building.handle.getContext();
		workspace.getUndoEngine().prepareContext(building.handle);
		strokeEngine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
		
		if( strokeEngine.startStroke( params, ps, building, workspace.getSelectionEngine().getSelection()))
			building.handle.refresh();
		return true;
	}

	@Override
	public void stepStroke(PenState ps) {
		if(strokeEngine.stepStroke(ps))
			strokeEngine.getImageData().handle.refresh();
	}

	@Override
	public void endStroke() {
		ImageWorkspace workspace = building.handle.getContext();
		UndoEngine undoEngine = workspace.getUndoEngine();
		strokeEngine.endStroke();
		
		final PenState[] states = strokeEngine.getHistory();
		final StrokeParams params = StrokeParams.Companion.bakeAndNormalize(strokeEngine.getParams(), states);
		final boolean behind = params.getMode() == PenDrawMode.BEHIND;
		final MagLevStroke stroke = new MagLevStroke(states, params);
		
		if( behind)
			params.setMode(PenDrawMode.NORMAL);
		
		//final SelectionMask mask = null;
		//final StrokeEngine _engine = strokeEngine;
		
		// NOTE: Rather unusual to prepare, perform, then store.
		//	Should really change strokes from writing automatically since 
		//	they're batched anyway (At least in GL Mode)
		img.addThing(stroke, behind);
		undoEngine.storeAction(new ImageAction(building) {
			@Override
			protected void performImageAction(BuiltMediumData built) {
				ImageWorkspace ws = built.getHandle().getContext();
				MaglevMedium mimg = (MaglevMedium)ws.getData(building.handle);
				mimg.Build();
				
				mimg.addThing(stroke, behind);

				mimg.getBuiltImage().doOnGC((gc) -> {
					stroke._draw(mimg.build(new BuildingMediumData(building.handle, 0, 0)), null, null, mimg, behind);
				}, new MatTrans());
			}
			@Override
			public String getDescription() {
				return "Stroke Action on MagLev Image";
			}
		});
		
		strokeEngine = null;
	}

	// :::: ITransformModule
	@Override
	public void transform(MatTrans trans) {
		building.handle.getContext().getUndoEngine().performAndStore(new ImageAction(building) {
			@Override
			protected void performImageAction(BuiltMediumData built) {
				ImageWorkspace ws = built.getHandle().getContext();
				MaglevMedium mimg = (MaglevMedium)ws.getData(built.getHandle());
				for( AMagLevThing things : mimg.getThings()) {
					things.transform(trans);		
				}
				mimg.unbuild();
				mimg.Build();
			}
		});
	}

	
	// :::: IMagnetifFillModule
	public static class BuildingStrokeSegment {
		public int strokeIndexAbs;
		public int pivot;
		public int travel;
		public BuildingStrokeSegment() {}
		
		public StrokeSegment build( List<AMagLevThing> things) {
			MagLevStroke stroke = (MagLevStroke) things.get(strokeIndexAbs);
			
			float start = stroke.getDirect().getT()[pivot];
			float end = stroke.getDirect().getT()[pivot+travel];
			
			return new StrokeSegment( stroke.getId(), start, end);
		}
	}
	BuildingStrokeSegment ss;
	List<BuildingStrokeSegment> strokeSegments;
	@Override public void startMagneticFill() { 
		ss = null;
		strokeSegments = new ArrayList<>();
	}
	@Override public void endMagneticFill( int color, MagneticFillMode mode) {
		List<StrokeSegment> toInsert = new ArrayList<>(strokeSegments.size());
		
		for( BuildingStrokeSegment segment : strokeSegments)
			toInsert.add( segment.build(img.getThings()));
		
		MagLevFill fill = new MagLevFill( toInsert, color);
		
		final boolean behind = (mode == MagneticFillMode.BEHIND);
		
		building.handle.getContext().getUndoEngine().performAndStore(new ImageAction(building) {
			@Override
			protected void performImageAction(BuiltMediumData built) {
				ImageWorkspace ws = built.getHandle().getContext();
				MaglevMedium mimg = (MaglevMedium)ws.getData(built.getHandle());
				mimg.Build();
				
				mimg.addThing(fill, true);

				mimg.getBuiltImage().doOnGC((gc) -> {
					fill._draw(mimg.build(new BuildingMediumData(built.getHandle(), 0, 0)), null, gc, mimg, behind);
				}, new MatTrans());
				
			}
			@Override
			public String getDescription() {
				return "Fill Action on MagLev Image";
			}
		});
		
		ss = null;
		strokeSegments = null;
	}

	@Override
	public void anchorPoints(float x, float y, float r, boolean locked, boolean relooping) {
		if( ss == null) {
			// [Behavior Part 1]: Latching onto a stroke.  Before it starts filling, first
			//	it has to find a stroke.  It'll latch onto to the first stroke it touches
			//	(the closest if it touches multiple stries).
			BuildingStrokeSegment cloestSegment = new BuildingStrokeSegment();
			float closestDistance = findClosestStroke( x, y, cloestSegment);
			
			if( cloestSegment.strokeIndexAbs != -1 && closestDistance <= r) {
				ss = cloestSegment;
				strokeSegments.add(ss);
			}
		}
		if( ss != null) {
			BuildingStrokeSegment closestSegment = new BuildingStrokeSegment();
			float closestDistance = findClosestStroke( x, y, closestSegment);
			
			if( closestSegment.strokeIndexAbs == ss.strokeIndexAbs && closestDistance <= r) {
				MagLevStroke stroke = (MagLevStroke) img.getThings().get(ss.strokeIndexAbs);
				DrawPoints direct = stroke.getDirect();
				
				if( Math.abs(ss.travel - (closestSegment.pivot - ss.pivot) * StrokeEngine.Companion.getDIFF()) >
					1.5 * MUtil.distance(direct.getX()[ss.pivot + ss.travel], direct.getY()[ss.pivot + ss.travel],
							direct.getX()[closestSegment.pivot], direct.getY()[closestSegment.pivot]) && !locked)
				{
					ss = closestSegment;
					strokeSegments.add(ss);
				}
				else
					ss.travel = closestSegment.pivot - ss.pivot;
			}
			else if( !locked &&closestSegment.strokeIndexAbs != -1 && closestDistance <= r) {
				ss = closestSegment;
				strokeSegments.add(ss);
			}
		}
	}
	private float findClosestStroke( float x, float y, BuildingStrokeSegment out)
	{
		float closestDistance = Float.MAX_VALUE;
		int closestIndex = -1;
		int closestStrokeIndex = -1;
		for(int thingIndex = 0; thingIndex < img.getThings().size(); ++thingIndex) {
			AMagLevThing thing = img.getThings().get(thingIndex);
			if( thing instanceof MagLevStroke) {
				MagLevStroke stroke = (MagLevStroke)thing;
				DrawPoints direct = stroke.getDirect();
				for(int i = 0; i < direct.getLength(); ++i) {
					float distance = (float) MUtil.distance(x, y, direct.getX()[i], direct.getY()[i]);
					if( distance < closestDistance) {
						closestIndex = i;
						closestStrokeIndex = thingIndex;
						closestDistance = distance;
					}
				}
			}
		}
		
		out.pivot = closestIndex;
		out.strokeIndexAbs = closestStrokeIndex;
		return closestDistance;
	}
	
	@Override
	public float[] getMagFillXs() {
		int totalLen = 0;
		for( BuildingStrokeSegment s : strokeSegments)
			totalLen += Math.abs(s.travel) + 1;
		
		float[] out = new float[totalLen];
		int index = 0;
		for( BuildingStrokeSegment s : strokeSegments) {
			MagLevStroke stroke = (MagLevStroke) img.getThings().get(s.strokeIndexAbs);
			DrawPoints direct = stroke.getDirect();
			for( int c=0; c <= Math.abs(s.travel); ++c) 
				out[index++] = direct.getX()[s.pivot + c * ((s.travel > 0)? 1 : -1)];
		}
		
		
		return out;
	}

	@Override
	public float[] getMagFillYs() {
		int totalLen = 0;
		for( BuildingStrokeSegment s : strokeSegments)
			totalLen += Math.abs(s.travel) + 1;
		
		float[] out = new float[totalLen];
		int index = 0;
		for( BuildingStrokeSegment s : strokeSegments) {
			MagLevStroke stroke = (MagLevStroke) img.getThings().get(s.strokeIndexAbs);
			DrawPoints direct = stroke.getDirect();
			for( int c=0; c <= Math.abs(s.travel); ++c)
				out[index++] = direct.getY()[s.pivot + c * ((s.travel > 0)? 1 : -1)];
		}
		
		return out;
	}

	@Override
	public void erasePoints( float x, float y, float r) {
		
	}

	// ::: IWeightEraserModule
	@Override public void startWeightErase(boolean precise) {
		eraserModule.startWeightErase(precise);
	}
	@Override public void endWeightErase() {
		eraserModule.endWeightErase();
	}
	@Override public void weightErase(float x, float y, float w) {
		eraserModule.weightErase(x,y,w);
	}

	// :::: IBoneDrawer
	@Override
	public void contort(BaseBone bone, Interpolator2D to) {

		ImageWorkspace ws = building.handle.getContext();
		ws.getUndoEngine().performAndStore(new ImageAction(building) {

			@Override
			protected void performImageAction(BuiltMediumData built) {
				MaglevMedium mimg = (MaglevMedium)ws.getData(built.getHandle());
				mimg.contortBones(bone, to);
			}
		});
	}

	// :::: IColorChangeModule
	@Override
	public void changeColor(int from, int to, ToolSchemes.ColorChangeScopes scope, ToolSchemes.ColorChangeMode mode) {
		ImageWorkspace ws = building.handle.getContext();
		final Map<Integer,Integer> oldColorMap = new HashMap<>();
		
		for(int i = 0; i < img.getThings().size(); ++i) {
			AMagLevThing thing = img.getThings().get(i);
			Integer color = null;
			
			
			if( thing instanceof MagLevFill) 
				color = ((MagLevFill)thing).color;
			if( thing instanceof MagLevStroke)
				color =((MagLevStroke) thing).params.getColor();
			
			switch( mode) {
			case CHECK_ALL:
				if( color == from)
					oldColorMap.put(i, color);
				break;
			case IGNORE_ALPHA:
				if( color != null && ((color & 0xfffff) == (from & 0xffffff)))
					oldColorMap.put(i, color);
				break;
			case AUTO:
				oldColorMap.put(i, color);
				break;
			}
		}
		
		if( oldColorMap.size() != 0) {
			ws.getUndoEngine().performAndStore(new ImageAction(building) {

				@Override
				protected void performImageAction(BuiltMediumData built) {
					MaglevMedium mimg = (MaglevMedium)ws.getData(built.getHandle());

					for( Entry<Integer,Integer> entry : oldColorMap.entrySet()) {
						int index = entry.getKey();
						AMagLevThing thing = mimg.getThings().get(index);
						AMagLevThing dupeThing = thing.clone();
						if( dupeThing instanceof MagLevFill)
							((MagLevFill)dupeThing).color = to;
						else if( dupeThing instanceof MagLevStroke)
							((MagLevStroke) dupeThing).params.setColor(to);
						
						mimg.getThings().remove(index);
						mimg.getThings().add(index, dupeThing);
					}
					
					mimg.unbuild();
					mimg.Build();
				}
			});
		}
	}

	// :::: ILiftSelectionModule, IAnchorLiftModule
	@Override public boolean acceptsLifted(ALiftedData lifted) {
		return liftModule.acceptsLifted(lifted);
	}
	@Override public void anchorLifted(ALiftedData lifted, MatTrans trans) {
		liftModule.anchorLifted(lifted, trans);
	}
	@Override public ALiftedData liftSelection(SelectionMask selection) {
		return liftModule.liftSelection(selection);
	}
}
