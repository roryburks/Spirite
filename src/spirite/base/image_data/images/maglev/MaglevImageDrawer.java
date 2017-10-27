package spirite.base.image_data.images.maglev;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.gl.GLGeom.Primitive;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.images.ABuiltImageData;
import spirite.base.image_data.images.drawer.IImageDrawer;
import spirite.base.image_data.images.drawer.IImageDrawer.*;
import spirite.base.image_data.images.maglev.MaglevInternalImage.MagLevFill;
import spirite.base.image_data.images.maglev.MaglevInternalImage.MagLevFill.StrokeSegment;
import spirite.base.image_data.images.maglev.MaglevInternalImage.MagLevStroke;
import spirite.base.image_data.images.maglev.MaglevInternalImage.MagLevThing;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;

public class MaglevImageDrawer 
	implements 	IImageDrawer,
				IStrokeModule,
				ITransformModule,
				IMagneticFillModule,
				IWeightEraserModule
{

	private final BuildingImageData building;
	private final MaglevInternalImage img;
	
	public MaglevImageDrawer( MaglevInternalImage img, BuildingImageData building) {
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
		
		strokeEngine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
		
		if( strokeEngine.startStroke( params, ps, workspace.buildData(building), workspace.getSelectionEngine().getBuiltSelection()))
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
		
		
		undoEngine.performAndStore(new ImageAction(building) {
			@Override
			protected void performImageAction() {
				ImageWorkspace ws = builtImage.handle.getContext();
				MaglevInternalImage mimg = (MaglevInternalImage)ws.getData(builtImage.handle);
				mimg.Build();
				
				mimg.addThing(stroke);
				ABuiltImageData built = building.handle.getContext().buildData(builtImage);
				StrokeEngine _engine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
				_engine.batchDraw(params, states, built, mask);
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
	List<StrokeSegment> strokeSegments;
	@Override public void startMagneticFill() { 
		ss = null;
		strokeSegments = new ArrayList<>();
	}
	@Override public void endMagneticFill( int color) {
		MagLevFill fill = new MagLevFill( strokeSegments, color);
		
		building.handle.getContext().getUndoEngine().performAndStore(new ImageAction(building) {
			@Override
			protected void performImageAction() {
				ImageWorkspace ws = builtImage.handle.getContext();
				MaglevInternalImage mimg = (MaglevInternalImage)ws.getData(builtImage.handle);
				mimg.Build();
				
				mimg.addThing(fill);
				
				GraphicsContext gc = mimg.builtImage.getGraphics();
				fill.draw(img.build(new BuildingImageData(building.handle, 0, 0)), null, gc, mimg);
				
			}
			@Override
			public String getDescription() {
				return "Fill Action on MagLev Image";
			}
		});
		
		ss = null;
		strokeSegments = null;
	}

	private static final double MAX_JUMP = 50;
	@Override
	public void anchorPoints(float x, float y, float r) {
		if( ss == null) {
			// [Behavior Part 1]: Latching onto a stroke.  Before it starts filling, first
			//	it has to find a stroke.  It'll latch onto to the first stroke it touches
			//	(the closest if it touches multiple stries).
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
			
			System.out.println(closestIndex);
			
			if( closestStrokeIndex != -1 && closestDistance <= r) {
				ss = new StrokeSegment();
				ss.pivot = closestIndex;
				ss.strokeIndex = closestStrokeIndex;
				ss.travel = 0;
				strokeSegments.add(ss);
			}
		}
		if( ss != null) {
			MagLevStroke stroke = (MagLevStroke)img.things.get(ss.strokeIndex);
			int sLen = stroke.direct.length;
			int abovePivot = 0;
			int belowPivot = 0;
			int found;
			for( found = -1; ss.pivot + abovePivot < sLen; ++abovePivot) {
				int index = abovePivot + ss.pivot;
				if( MUtil.distance( x, y, stroke.direct.x[index], stroke.direct.y[index]) < r)
					found = abovePivot;
				else if( found > ss.travel)	// Quit if you found a point and broke it
					break;
				
				// Don't Look for connections above MAX_JUMP distance from the pivot point
				if( (abovePivot - Math.max(ss.travel, 0))* StrokeEngine.DIFF >  +  MAX_JUMP)
					break;
			}
			abovePivot = found;
			
			for( found = -1; ss.pivot - belowPivot >= 0; ++belowPivot) {
				int index = ss.pivot - belowPivot;

				if( MUtil.distance( x, y, stroke.direct.x[index], stroke.direct.y[index]) < r)
					found = belowPivot;
				else if( found > -ss.travel)	// Quit if you found a point and broke it
					break;
				
				// Don't Look for connections above MAX_JUMP distance from the pivot point
				if( (belowPivot - Math.max(Math.abs(ss.travel), 0))* StrokeEngine.DIFF >  +  MAX_JUMP)
					break;
			}
			belowPivot = found;
			
			if( abovePivot == -1 && belowPivot == -1) {
				// No matches found, look for a jump
				// TODO
			}
			else
				ss.travel = (abovePivot > belowPivot) ? abovePivot : -belowPivot;
		}
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
			for( int c=0; c <= Math.abs(s.travel); ++c) {
				if( s.pivot + c * ((s.travel > 0)? 1 : -1) < 0 || s.pivot + c * ((s.travel > 0)? 1 : -1) > stroke.direct.length)
					System.out.println("break");
				out[index++] = stroke.direct.x[s.pivot + c * ((s.travel > 0)? 1 : -1)];
			}
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

	@Override
	public void interpretFill(float[] curve, int color) {
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
		MagWeightEraseAction(BuildingImageData data, List<MagLevThing> thingsToErase, int id)
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
		protected void performImageAction() {
			ImageWorkspace ws = builtImage.handle.getContext();
			MaglevInternalImage mimg = (MaglevInternalImage)ws.getData(builtImage.handle);
			
			for( MagLevThing toErase : thingsToErase) {
				mimg.things.remove(toErase);
			}
			mimg.unbuild();
			builtImage.handle.refresh();
		}
		@Override
		public String getDescription() {
			return "Erase Mag Stroke";
		}
	}

}
