package spirite.base.image_data.images.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.gl.GLGeom.Primitive;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.ImageAction;
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
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;

public class MaglevImageDrawer 
	implements 	IImageDrawer,
				IStrokeModule,
				ITransformModule,
				IMagneticFillModule
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
	@Override public void startMagneticFill() {}

	@Override public void endMagneticFill() {}

	@Override
	public float[][] anchorPoints(float x1, float y1, float r1, float x2, float y2, float r2) {
		return new float[][] {{x1,y1}};
	}

	@Override
	public float[][] anchorPointsHard(float x, float y, float r) {
		return new float[][] {{x,y}};
	}

	@Override
	public void interpretFill(float[] curve, int color) {
		float[] x = new float[curve.length/2];
		float[] y = new float[curve.length/2];
		for( int i=0; i < x.length; ++i) {
			x[i] = curve[i*2];
			y[i] = curve[i*2+1];
		}
		MagLevFill fill = new MagLevFill(x, y, color);
		

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
	}
}
