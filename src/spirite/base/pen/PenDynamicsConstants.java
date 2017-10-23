package spirite.base.pen;

import java.util.Arrays;

import spirite.base.pen.PenTraits.PenDynamics;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.util.glmath.Vec2;

/***
 * Pretty much anything which alters the image data directly goes 
 * through the DrawEngine.
 * 
 * @author Rory Burks
 *
 */
public class PenDynamicsConstants {
	// ==============
	// ==== Stroke Dynamics
	// TODO: This should probably be in some settings area
	
	public static PenDynamics getBasicDynamics() {
		return basicDynamics;
	}
	private static final PenDynamics basicDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};

	private static final PenDynamics personalDynamics = new PenTraits.LegrangeDynamics(
		Arrays.asList( new Vec2[] {
				new Vec2(0,0),
				new Vec2(0.25f,0),
				new Vec2(1,1)
			}
		)
	);
	
	public static PenDynamics LinearDynamics() {
		return personalDynamics;
	}
	private static final PenDynamics defaultDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};
}
