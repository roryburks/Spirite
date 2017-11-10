package spirite.base.pen.behaviors;

import spirite.base.pen.Penner;

public class RotatingReferenceBehavior extends StateBehavior {
	public RotatingReferenceBehavior(Penner penner) {
		super(penner);
	}
	int startx = this.penner.x;
	int starty = this.penner.y;
	int ox = this.penner.rawX;
	@Override public void start() {}
	@Override public void onTock() {}
	@Override
	public void onMove() {
		float theta = (this.penner.rawX-ox)*0.05f;
		ox = this.penner.rawX;
		this.penner.workspace.getReferenceManager().rotateTransform(theta, startx, starty);
	}
}