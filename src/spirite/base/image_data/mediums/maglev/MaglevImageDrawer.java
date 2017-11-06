package spirite.base.image_data.mediums.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IMagneticFillModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IStrokeModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.ITransformModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IWeightEraserModule;
import spirite.base.image_data.mediums.maglev.MaglevMedium.MagLevFill;
import spirite.base.image_data.mediums.maglev.MaglevMedium.MagLevStroke;
import spirite.base.image_data.mediums.maglev.MaglevMedium.MagLevThing;
import spirite.base.image_data.mediums.maglev.MaglevMedium.MagLevFill.StrokeSegment;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;

public class MaglevImageDrawer 
	implements 	IImageDrawer,
				IStrokeModule,
				ITransformModule,
				IMagneticFillModule,
				IWeightEraserModule
{

	private final BuildingMediumData building;
	private final MaglevMedium img;
	
	public MaglevImageDrawer( MaglevMedium img, BuildingMediumData building) {
		this.img = img;
		this.building = building;
	}

	
	// ::: IStrokeModule
	StrokeEngine strokeEngine = null;
	@Override public StrokeEngine getStrokeEngine() {return strokeEngine;}

	@Override
	public boolean canDoStroke(StrokeParams params) {
		return true;
	}

	@Override
	public boolean startStroke(StrokeParams params, PenState ps) {
		ImageWorkspace workspace = building.handle.getContext();
		workspace.getUndoEngine().prepareContext(building.handle);
		strokeEngine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
		
		if( strokeEngine.startStroke( params, ps, building, workspace.getSelectionEngine().getBuiltSelection()))
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
		final StrokeParams params = StrokeParams.bakeAndNormalize(strokeEngine.getParams(), states);
		final MagLevStroke stroke = new MagLevStroke(states, params);
		final BuiltSelection mask = new BuiltSelection(null, 0, 0);
		//final StrokeEngine _engine = strokeEngine;
		
		// NOTE: Rather unusual to prepare, perform, then store.
		//	Should really change strokes from writing automatically since 
		//	they're batched anyway (At least in GL Mode)
		img.addThing(stroke);
		undoEngine.storeAction(new ImageAction(building) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				ImageWorkspace ws = built.handle.getContext();
				MaglevMedium mimg = (MaglevMedium)ws.getData(building.handle);
				mimg.Build();
				
				mimg.addThing(stroke);

				GraphicsContext gc = mimg.builtImage.checkout(new MatTrans());
				stroke.draw(mimg.build(new BuildingMediumData(building.handle, 0, 0)), null, gc, mimg);
				mimg.builtImage.checkin();
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
		for( MagLevThing things : img.things) {
			float xy[] = things.getPoints();
			if( xy == null) continue;
			for( int i=0; i < xy.length/2; ++i) {
				Vec2 to = trans.transform(new Vec2(xy[i*2],xy[i*2+1]), new Vec2(0,0));
				xy[i*2] = to.x;
				xy[i*2+1] = to.y;
			}
			things.setPoints(xy);
		}
		img.unbuild();
		building.handle.refresh();
	}

	
	// :::: IMagnetifFillModule
	StrokeSegment ss;
	int floatSoft;
	List<StrokeSegment> strokeSegments;
	@Override public void startMagneticFill() { 
		ss = null;
		strokeSegments = new ArrayList<>();
	}
	@Override public void endMagneticFill( int color) {
		MagLevFill fill = new MagLevFill( strokeSegments, color);
		
		building.handle.getContext().getUndoEngine().performAndStore(new ImageAction(building) {
			@Override
			protected void performImageAction(ABuiltMediumData built) {
				ImageWorkspace ws = built.handle.getContext();
				MaglevMedium mimg = (MaglevMedium)ws.getData(built.handle);
				mimg.Build();
				
				mimg.addThing(fill);

				GraphicsContext gc = mimg.builtImage.checkout(new MatTrans());
				fill.draw(mimg.build(new BuildingMediumData(building.handle, 0, 0)), null, gc, mimg);
				mimg.builtImage.checkin();
				
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
			StrokeSegment cloestSegment = new StrokeSegment();
			float closestDistance = findClosestStroke( x, y, cloestSegment);
			
			if( cloestSegment.strokeIndex != -1 && closestDistance <= r) {
				ss = cloestSegment;
				strokeSegments.add(ss);
			}
		}
		if( ss != null) {
			StrokeSegment closestSegment = new StrokeSegment();
			float closestDistance = findClosestStroke( x, y, closestSegment);
			
			if( closestSegment.strokeIndex == ss.strokeIndex && closestDistance <= r) {
				MagLevStroke stroke = (MagLevStroke) img.things.get(ss.strokeIndex);
				
				if( Math.abs(ss.travel - (closestSegment.pivot - ss.pivot) * StrokeEngine.DIFF) > 
					1.5 * MUtil.distance(stroke.direct.x[ss.pivot + ss.travel], stroke.direct.y[ss.pivot + ss.travel],
							stroke.direct.x[closestSegment.pivot], stroke.direct.y[closestSegment.pivot]) && !locked)
				{
					ss = closestSegment;
					strokeSegments.add(ss);
				}
				else
					ss.travel = closestSegment.pivot - ss.pivot;
			}
			else if( !locked &&closestSegment.strokeIndex != -1 && closestDistance <= r) {
				ss = closestSegment;
				strokeSegments.add(ss);
			}
//			// [Behavior Part 2]: When latched on, the 
//			MagLevStroke stroke = (MagLevStroke)img.things.get(ss.strokeIndex);
//			int sLen = stroke.direct.length;
//			int abovePivot = 0;
//			int belowPivot = 0;
//			int found;
//			for( found = -1; ss.pivot + abovePivot < sLen; ++abovePivot) {
//				int index = abovePivot + ss.pivot;
//				if( MUtil.distance( x, y, stroke.direct.x[index], stroke.direct.y[index]) < r)
//					found = abovePivot;
//				else if( found > ss.travel)	// Quit if you found a point and broke it
//					break;
//				
//				// Don't Look for connections above MAX_JUMP distance from the pivot point
//				if( (abovePivot - Math.max(ss.travel, 0))* StrokeEngine.DIFF >  +  MAX_JUMP)
//					break;
//			}
//			abovePivot = found;
//			
//			for( found = -1; ss.pivot - belowPivot >= 0; ++belowPivot) {
//				int index = ss.pivot - belowPivot;
//
//				if( MUtil.distance( x, y, stroke.direct.x[index], stroke.direct.y[index]) < r)
//					found = belowPivot;
//				else if( found > -ss.travel)	// Quit if you found a point and broke it
//					break;
//				
//				// Don't Look for connections above MAX_JUMP distance from the pivot point
//				if( (belowPivot - Math.max(Math.abs(ss.travel), 0))* StrokeEngine.DIFF >  +  MAX_JUMP)
//					break;
//			}
//			belowPivot = found;
//			
//			if( abovePivot == -1 && belowPivot == -1) {
//				// No matches found, look for a jump
//				// TODO: Make this better, so that it latches on to things closer to where the segment should
//				//	logically break.
//				StrokeSegment closestSegment = new StrokeSegment();
//				float closestDistance = findClosestStroke( x, y, closestSegment);
//				
//				if( closestSegment.strokeIndex != -1 && closestDistance <= r
//						&& (ss == null || closestSegment.strokeIndex != ss.strokeIndex)) 
//				{
//					ss = closestSegment;
//					strokeSegments.add(ss);
//				}
//			}
//			else
//				ss.travel = (abovePivot > belowPivot) ? abovePivot : -belowPivot;
		}
	}
	private float findClosestStroke( float x, float y, StrokeSegment out)
	{
		float closestDistance = Float.MAX_VALUE;
		int closestIndex = -1;
		int closestStrokeIndex = -1;
		for( int thingIndex = 0; thingIndex < img.things.size(); ++thingIndex) {
			MagLevThing thing = img.things.get(thingIndex);
			if( thing instanceof MagLevStroke) {
				MagLevStroke stroke = (MagLevStroke)thing;
				for( int i=0; i < stroke.direct.length; ++i) {
					float distance = (float) MUtil.distance(x, y, stroke.direct.x[i], stroke.direct.y[i]);
					if( distance < closestDistance) {
						closestIndex = i;
						closestStrokeIndex = thingIndex;
						closestDistance = distance;
					}
				}
			}
		}
		
		out.pivot = closestIndex;
		out.strokeIndex = closestStrokeIndex;
		return closestDistance;
	}
	
	@Override
	public float[] getMagFillXs() {
		int totalLen = 0;
		for( StrokeSegment s : strokeSegments)
			totalLen += Math.abs(s.travel) + 1;
		
		float[] out = new float[totalLen];
		int index = 0;
		for( StrokeSegment s : strokeSegments) {
			MagLevStroke stroke = (MagLevStroke)img.things.get(s.strokeIndex);
			for( int c=0; c <= Math.abs(s.travel); ++c) 
				out[index++] = stroke.direct.x[s.pivot + c * ((s.travel > 0)? 1 : -1)];
		}
		
		
		return out;
	}

	@Override
	public float[] getMagFillYs() {
		int totalLen = 0;
		for( StrokeSegment s : strokeSegments)
			totalLen += Math.abs(s.travel) + 1;
		
		float[] out = new float[totalLen];
		int index = 0;
		for( StrokeSegment s : strokeSegments) {
			MagLevStroke stroke = (MagLevStroke)img.things.get(s.strokeIndex);
			for( int c=0; c <= Math.abs(s.travel); ++c)
				out[index++] = stroke.direct.y[s.pivot + c * ((s.travel > 0)? 1 : -1)];
		}
		
		return out;
	}

	// ::: IWeightEraserModule
	static Object iweIDLocker = new Object();
	static int iweIDCounter = 0;
	int iweId = -1;
	@Override
	public void weightErase(float x, float y, float w) {
		List<MagLevThing> thingsToRemove = new ArrayList<>();
		for( MagLevThing thing : img.things) {
			if( thing instanceof MagLevStroke) {
				MagLevStroke stroke = (MagLevStroke)thing;

				for( int i=0; i < stroke.direct.length; ++i) {
					if( MUtil.distance(x, y, stroke.direct.x[i], stroke.direct.y[i]) < w) {
						thingsToRemove.add(stroke);
						break;
					}
				}
			}
		}
		
		if( thingsToRemove.size() > 0) {
			this.building.handle.getContext().getUndoEngine().performAndStore(
					new MagWeightEraseAction(building, thingsToRemove, iweId));
		}
//		
	}

	@Override
	public void startWeightErase() {
		synchronized( iweIDLocker) {
			iweId = iweIDCounter++;
		}
	}
	@Override
	public void endWeightErase() {
		iweId = -1;
	}
	class MagWeightEraseAction extends ImageAction implements StackableAction {
		private final List<MagLevThing> thingsToErase;
		private final int id;
		MagWeightEraseAction(BuildingMediumData data, List<MagLevThing> thingsToErase, int id)
		{
			super(data);
			this.id = id;
			this.thingsToErase = new ArrayList<>(1);
			this.thingsToErase.addAll(thingsToErase);
		}

		@Override public boolean canStack(UndoableAction newAction) {
			return ( newAction instanceof MagWeightEraseAction && ((MagWeightEraseAction) newAction).id == id);
		}
		@Override public void stackNewAction(UndoableAction newAction) {
			thingsToErase.addAll(((MagWeightEraseAction)newAction).thingsToErase);
		}

		@Override
		protected void performImageAction(ABuiltMediumData built) {
			ImageWorkspace ws = building.handle.getContext();
			MaglevMedium mimg = (MaglevMedium)ws.getData(building.handle);
			
			for( MagLevThing toErase : thingsToErase) {
				mimg.things.remove(toErase);
			}
			mimg.unbuild();
			building.handle.refresh();
		}
		@Override
		public String getDescription() {
			return "Erase Mag Stroke";
		}
	}

	@Override
	public void erasePoints( float x, float y, float r) {
		
	}

}
