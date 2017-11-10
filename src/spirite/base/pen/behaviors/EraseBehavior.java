package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;

public class EraseBehavior extends StrokeBehavior {
	public EraseBehavior(Penner penner) {
		super(penner);
	}

	@Override
	public void start() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.ERASER);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setMethod( StrokeEngine.Method.ERASE);
		stroke.setWidth((float)settings.getValue("width"));
		stroke.setHard((Boolean)settings.getValue("hard"));

		// Start the Stroke
		startStroke( stroke);
	}
}