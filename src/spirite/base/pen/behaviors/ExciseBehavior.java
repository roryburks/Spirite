package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IWeightEraserModule;
import spirite.base.pen.Penner;


public class ExciseBehavior extends StateBehavior { 
	private final IWeightEraserModule drawer;
	
	public ExciseBehavior( Penner penner, IWeightEraserModule drawer) {
		super(penner);
		this.drawer = drawer;
	}
	
	@Override public void start() {
		ToolSettings settings = penner.toolsetManager.getToolSettings(Tool.EXCISE_ERASER);
		drawer.startWeightErase((Boolean)settings.getValue("precies"));
		onMove();
	}
	@Override public void onTock() {}

	@Override
	public void onMove() {
		drawer.weightErase(this.penner.x, this.penner.y, (Float)this.penner.toolsetManager.getToolSettings(Tool.EXCISE_ERASER).getProperty("width").getValue());
	}
	
	@Override
	public void end() {
		drawer.endWeightErase();
		super.end();
	}
	
}