package spirite.base.pen;

import spirite.base.util.interpolation.LagrangeInterpolator;
import spirite.base.util.linear.Vec2;

import java.util.List;

public class PenTraits {

	public enum ButtonType {
		LEFT, RIGHT, CENTER
	}
	public static class MButtonEvent {
		public ButtonType buttonType;
		
	}
	
	public static class PenState {
		//public PenState(){}
		public PenState( float x, float y, float pressure) {
			this.x = x;
			this.y = y;
			this.pressure = pressure;
		}
		public PenState( PenState other) {
			this.x = other.x;
			this.y = other.y;
			this.pressure = other.pressure;
		}
		public final float x;
		public final float y;
		public final float pressure;// = 1.0f;
	}

	public static interface PenDynamics {
		public float getSize( PenState ps);
	}
	
	public static class LegrangeDynamics implements PenDynamics{
		private final LagrangeInterpolator li;
		public LegrangeDynamics( List<Vec2> list) {
			this.li = new LagrangeInterpolator(list);
		}

		@Override
		public float getSize(PenState ps) {
			return Math.min(1, Math.max(0, (float) li.eval(ps.pressure)));
		}
	}

}
