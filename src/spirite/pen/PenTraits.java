package spirite.pen;

import java.awt.geom.Point2D;
import java.util.List;

import mutil.Interpolation.LagrangeInterpolator;

public class PenTraits {

	public static class PenState {
		public PenState(){}
		public PenState( int x, int y, float pressure) {
			this.x = x;
			this.y = y;
			this.pressure = pressure;
		}
		public PenState( PenState other) {
			this.x = other.x;
			this.y = other.y;
			this.pressure = other.pressure;
		}
		public int x;
		public int y;
		public float pressure = 1.0f;
	}

	public static interface PenDynamics {
		public float getSize( PenState ps);
	}
	
	public static class LegrangeDynamics implements PenDynamics{
		private final LagrangeInterpolator li;
		public LegrangeDynamics( List<Point2D> list) {
			this.li = new LagrangeInterpolator(list);
		}

		@Override
		public float getSize(PenState ps) {
			System.out.println(ps.pressure);
			return Math.min(1, Math.max(0, (float) li.f(ps.pressure)));
		}
	}

}
