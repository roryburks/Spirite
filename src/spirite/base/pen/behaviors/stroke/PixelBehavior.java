package spirite.base.pen.behaviors.stroke;

import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.pen.StrokeEngine.StrokeParams.InterpolationMethod;

public class PixelBehavior {
	public static class Stroke extends StrokeBehavior {
		private final int color;
		
		public Stroke( Penner penner, int color) {
			super(penner);
			this.color = color;
		}

		@Override
		public StrokeParams makeStroke() {
			return _makeStroke(penner.toolsetManager, color);
		}
	}
	
	public static class Fixed extends FixedStrokeBehavior {
		private final int color;
		
		public Fixed( Penner penner, int color) {
			super(penner);
			this.color = color;
		}

		@Override
		public StrokeParams makeStroke() {
			return _makeStroke(penner.toolsetManager, color);
		}
	}

	private static StrokeParams _makeStroke(ToolsetManager toolsetManager, int color) {
		ToolSettings settings = toolsetManager.getToolSettings(Tool.PIXEL);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setMethod( StrokeEngine.Method.PIXEL);
		stroke.setAlpha((float)settings.getValue("alpha"));
		stroke.setHard(true);
		stroke.setColor( color);
		stroke.setInterpolationMethod(InterpolationMethod.NONE);
		
		return stroke;
	}
}