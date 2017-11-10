package spirite.base.pen.behaviors;

import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.Method;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.pen.StrokeEngine.StrokeParams.InterpolationMethod;

public class PixelBehavior extends StrokeBehavior {
	final int color;
	public PixelBehavior( Penner penner, int i) {
		super(penner);
		this.color = i;
	}
	@Override
	public void start() {
		ToolSettings settings = this.penner.toolsetManager.getToolSettings(Tool.PIXEL);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setMethod( StrokeEngine.Method.PIXEL);
		stroke.setAlpha((float)settings.getValue("alpha"));
		stroke.setHard(true);
		stroke.setColor( color);
		stroke.setInterpolationMethod(InterpolationMethod.NONE);
		startStroke( stroke);
	}
}