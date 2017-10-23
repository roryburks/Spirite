package spirite.base.image_data.images.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.CompositeAction;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.images.ABuiltImageData;
import spirite.base.image_data.images.drawer.IImageDrawer;
import spirite.base.image_data.images.drawer.DefaultImageDrawer.StrokeAction;
import spirite.base.image_data.images.drawer.IImageDrawer.*;
import spirite.base.image_data.images.maglev.MaglevInternalImage.MagLevStroke;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.Method;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;

public class MaglevImageDrawer 
	implements 	IImageDrawer,
				IStrokeModule,
				ITransformModule
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
		return params.getMethod() == Method.BASIC;
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
		final StrokeParams params = strokeEngine.getParams();
		final MagLevStroke stroke = img.new MagLevStroke(states, params);
		final BuiltSelection mask = new BuiltSelection(null, 0, 0);
		//final StrokeEngine _engine = strokeEngine;
		
		List<UndoableAction> actions = new ArrayList<>(2);
		actions.add(new UndoEngine.NullAction(){
			@Override
			protected void performAction() {
				img.addStroke(stroke);
			}

			@Override
			protected void undoAction() {
				img.popStroke();
				img.unbuild();
			}
		});
		actions.add( new ImageAction(building) {
			@Override
			protected void performImageAction() {
				ABuiltImageData built = building.handle.getContext().buildData(builtImage);
				StrokeEngine _engine = workspace.getSettingsManager().getDefaultDrawer().getStrokeEngine();
				_engine.batchDraw(params, states, built, mask);
			}
		});
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Stroke Action on MagLev Image"));
		
		strokeEngine = null;
	}

	@Override
	public void transform(MatTrans trans) {
		for( MagLevStroke stroke : img.strokes) {
			for( PenState ps : stroke.states) {
				Vec2 to = trans.transform(new Vec2(ps.x,ps.y), new Vec2(0,0));
				ps.x = to.x;
				ps.y = to.y;
			}
		}
		img.unbuild();
		building.handle.refresh();
	}
}
