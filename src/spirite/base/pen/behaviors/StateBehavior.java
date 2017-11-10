package spirite.base.pen.behaviors;

import spirite.base.pen.Penner;

// By design, StateBehavior has and should make use of all local variables
//	relevant to it, variables passed to it (if any) are for convenience only
//	as the StateBehavior could have either accessed them or caculated them
//	itself.
public abstract class StateBehavior {
	protected final Penner penner;

	public StateBehavior(Penner penner) {
		this.penner = penner;
	}
	public abstract void start();
	public abstract void onTock();
	public abstract void onMove();
	
	// For most StateBehavior, onPenDown will be irrelevant/not make sense
	//	because their penUp action is to cancel the state.
	public void onPenDown() {}
	public void onPenUp() {
		end();
	}
	
	public void end() {
		// This effectively ends the state behavior
		this.penner.behavior = null;
	}
}