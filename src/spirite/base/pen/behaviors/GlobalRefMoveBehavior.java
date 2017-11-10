package spirite.base.pen.behaviors;

import spirite.base.pen.Penner;

public class GlobalRefMoveBehavior extends StateBehavior {
	public GlobalRefMoveBehavior(Penner penner) {
		super(penner);
	}
	@Override public void start() {}
	@Override public void onTock() {}
	@Override
	public void onMove() {
		this.penner.workspace.getReferenceManager().shiftTransform((this.penner.x-this.penner.oldX), (this.penner.y-this.penner.oldY));
	}
}