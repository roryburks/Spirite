package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.PenDrawMode;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;

public class PenBehavior extends StrokeBehavior {
	final int color;
	public PenBehavior( Penner penner, int i) {
		super(penner);
		this.color = i;
	}
	@Override
	public void start() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.PEN);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setColor( color);
		stroke.setMode((PenDrawMode)settings.getValue("mode"));
		stroke.setWidth((float)settings.getValue("width"));
		stroke.setAlpha((float)settings.getValue("alpha"));
		stroke.setHard((Boolean)settings.getValue("hard"));
		
		// Start the Stroke
		startStroke( stroke);
	}
}