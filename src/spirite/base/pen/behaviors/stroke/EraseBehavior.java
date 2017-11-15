package spirite.base.pen.behaviors.stroke;

import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.pen.Penner;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.StrokeParams;

public class EraseBehavior {
	public static class Stroke extends StrokeBehavior {
		public Stroke(Penner penner) {
			super(penner);
		}

		@Override
		public StrokeParams makeStroke() {
			return _makeStroke(penner.toolsetManager);
		}
	}
	
	public static class Fixed extends FixedStrokeBehavior {
		public Fixed(Penner penner) {
			super(penner);
		}

		@Override
		public StrokeParams makeStroke() {
			return _makeStroke(penner.toolsetManager);
		}
		
	}
	
	private static StrokeParams _makeStroke(ToolsetManager toolsetManager) {
		ToolSettings settings = toolsetManager.getToolSettings(Tool.ERASER);
		StrokeEngine.StrokeParams stroke = new StrokeEngine.StrokeParams();
		stroke.setMethod( StrokeEngine.Method.ERASE);
		stroke.setWidth((float)settings.getValue("width"));
		stroke.setHard((Boolean)settings.getValue("hard"));
		
		return stroke;
	}

}