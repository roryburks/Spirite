package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.Penner;
import spirite.base.util.MUtil;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;
import spirite.base.util.linear.Vec2i;

public class ReshapingBehavior extends TransformBehavior {
	public ReshapingBehavior(Penner penner) {
		super(penner);
	}
	@Override
	public void start() {
		this.penner.selectionEngine.proposeTransform(new MatTrans());
		setState(TransormStates.READY);
	}
	@Override
	public void end() {
		this.penner.selectionEngine.proposeTransform(null);
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.RESHAPER);
		settings.setValue("scale", new Vec2(1, 1));
		settings.setValue("translation", new Vec2(0, 0));
		settings.setValue("rotation", (float)0);
		super.end();
	}

	@Override public void onTock() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.RESHAPER);
		Vec2 scale = (Vec2)settings.getValue("scale");
		Vec2 translation = (Vec2)settings.getValue("translation");
		this.scaleX = scale.x;
		this.scaleY = scale.y;
		this.translateX = translation.x;
		this.translateY = translation.y;
		this.rotation = (float)settings.getValue("rotation");

		SelectionMask sel = this.penner.selectionEngine.getSelection();
		if( this.penner.selectionEngine.getSelection() == null){
			this.end();
			return;
		}
		this.region = MUtil.circumscribeTrans(new Rect(0, 0, sel.getWidth(), sel.getHeight()), this.penner.selectionEngine.getLiftedDrawTrans(false));
		
		Vec2i d = sel.getDimension();
		

		this.penner.selectionEngine.proposeTransform(this.getWorkingTransform());
	}

	@Override public void onPenUp() {
		setState(TransormStates.READY);
	}
	@Override
	public void onPenDown() {
		SelectionMask sel =this.penner.selectionEngine.getSelection();
		
		if( sel == null){
			this.end();
			return;
		}
		
		if( overlap >= 0 && overlap <= 7)
			this.setState(TransormStates.RESIZE);
		else if( overlap >= 8 && overlap <= 0xB)
			this.setState(TransormStates.ROTATE);
		else if( overlap == 0xC)
			this.setState(TransormStates.MOVING);
	}

	@Override protected void onScaleChanged() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.RESHAPER);
		settings.setValue("scale", new Vec2(scaleX, scaleY));
	}
	@Override protected void onTrnalsationChanged() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.RESHAPER);
		settings.setValue("translation", new Vec2(translateX, translateY));
	}
	@Override protected void onRotationChanged() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.RESHAPER);
		settings.setValue("rotation", rotation);
	}
}