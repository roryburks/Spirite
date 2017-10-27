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
	FloatCompactor out_x;
	FloatCompactor out_y;
	private class StrokeSegment {
		MagLevStroke stroke;
		int start;
		int end;
		int direction = 0;	// 0 means unknown, 1 means forward, -1 means nil
	}
	@Override public void startMagneticFill() { 
		ss = null;
		out_x = new FloatCompactor();
		out_y = new FloatCompactor();
	}
	@Override public void endMagneticFill( int color) {
		MagLevFill fill = new MagLevFill(out_x.toArray(), out_y.toArray(), color);
		

		building.handle.getContext().getUndoEngine().performAndStore(new ImageAction(building) {
			@Override
			protected void performImageAction() {
				ImageWorkspace ws = builtImage.handle.getContext();
				MaglevInternalImage mimg = (MaglevInternalImage)ws.getData(builtImage.handle);
				mimg.Build();
				
				mimg.addThing(fill);
				GraphicsContext gc = mimg.builtImage.getGraphics();
				gc.setColor(color);
				gc.fillPolygon(fill.x, fill.y, fill.x.length);
				
			}
			@Override
			public String getDescription() {
				return "Fill Action on MagLev Image";
			}
		});
		
		ss = null;
		out_x = null;
		out_y = null;
	}

	@Override
	public void anchorPoints(float x1, float y1, float r1, float x2, float y2, float r2) {
		if( ss == null) {
			// Find the closest stroke to anchor to
			float closestDistance = r1+0.0001f;
			int closestIndex = -1;
			MagLevStroke closestStroke = null;
			for( MagLevThing thing : img.things) {
				if( thing instanceof MagLevStroke) {
					MagLevStroke stroke = (MagLevStroke)thing;
					for( int i=0; i < stroke.states.length; ++i) {
						float distance = (float) MUtil.distance(x1, y1, stroke.states[i].x, stroke.states[i].y);
						if( distance < closestDistance) {
							closestIndex = i;
							closestStroke = stroke;
							closestDistance = distance;
						}
					}
				}
			}
			
			if( closestStroke != null) {
				ss = new StrokeSegment();
				ss.start = ss.end = closestIndex;
				ss.stroke = closestStroke;
			}
		}
		if( ss != null) {
			if( ss.direction == 0) {
				boolean soft = false;
				for( int i=ss.start; i >=0; --i) {
					if( (float) MUtil.distance(x1, y1, ss.stroke.states[i].x, ss.stroke.states[i].y) < r1) {
						ss.start = i;
						soft = true;
					}
					else break;
				}
				for( int i=ss.end; i <ss.stroke.states.length; ++i) {
					if( (float) MUtil.distance(x1, y1, ss.stroke.states[i].x, ss.stroke.states[i].y) < r1) {
						ss.end= i;
						soft = true;
					}
					else break;
				}
				if( !soft) {
					if(MUtil.distance(x2, y2, ss.stroke.states[ss.start].x, ss.stroke.states[ss.start].y) < 
							MUtil.distance(x2, y2, ss.stroke.states[ss.end].x, ss.stroke.states[ss.end].y)) 
					{
						ss.direction = -1;
						for( int i=ss.end; i >= ss.start; --i) {
							out_x.add(ss.stroke.states[i].x);
							out_y.add(ss.stroke.states[i].y);
						}
					}
					else {
						ss.direction = 1;
						for( int i=ss.start; i <= ss.end; ++i) {
							out_x.add(ss.stroke.states[i].x);
							out_y.add(ss.stroke.states[i].y);
						}
					}
					
				}
			}
			else {
				boolean hard = false;
				if( ss.direction == -1) {
					for( int i= ss.start; i >= 0; --i) {
						if( (float) MUtil.distance(x2, y2, ss.stroke.states[i].x, ss.stroke.states[i].y) < r2) { 
							hard = true;
							if( ss.start != i) {
								out_x.add(ss.stroke.states[i].x);
								out_y.add(ss.stroke.states[i].y);
							}
								
							ss.start = i;
						}
					}
				}
				if( ss.direction == 1) {
					for( int i= ss.end; i < ss.stroke.states.length ; ++i) {
						if( (float) MUtil.distance(x2, y2, ss.stroke.states[i].x, ss.stroke.states[i].y) < r2) { 
							hard = true;
							if( ss.end != i) {
								out_x.add(ss.stroke.states[i].x);
								out_y.add(ss.stroke.states[i].y);
							}
							ss.end = i;
						}
					}
				}
				if( !hard) {
					// TODO
				}
			}
		}
	}
	@Override
	public float[] getXs() {
		return out_x.toArray();
	}

	@Override
	public float[] getYs() {
		return out_y.toArray();
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
