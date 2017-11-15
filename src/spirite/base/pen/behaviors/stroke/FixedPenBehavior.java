package spirite.base.pen.behaviors.stroke;

import spirite.base.brains.ToolsetManager.PenDrawMode;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;

public class FixedPenBehavior extends FixedStrokeBehavior {
	final int color;

	public FixedPenBehavior(Penner penner, int color) {
		super(penner);
		this.color = color;
	}

	@Override
	public StrokeParams makeStroke() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.PEN);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setColor( color);
		stroke.setMode((PenDrawMode)settings.getValue("mode"));
		stroke.setWidth((float)settings.getValue("width"));
		stroke.setAlpha((float)settings.getValue("alpha"));
		stroke.setHard((Boolean)settings.getValue("hard"));
		
		return stroke;
	}

}
