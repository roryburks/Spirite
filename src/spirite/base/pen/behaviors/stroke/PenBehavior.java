package spirite.base.pen.behaviors.stroke;

import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.PenDrawMode;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;

public class PenBehavior  {
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
		ToolSettings settings = toolsetManager.getToolSettings(Tool.PEN);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setColor( color);
		stroke.setMode((PenDrawMode)settings.getValue("mode"));
		stroke.setWidth((float)settings.getValue("width"));
		stroke.setAlpha((float)settings.getValue("alpha"));
		stroke.setHard((Boolean)settings.getValue("hard"));
		
		return stroke;
	}
}