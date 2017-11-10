package spirite.base.pen.behaviors;

import spirite.base.pen.Penner;

public class ZoomingReferenceBehavior extends StateBehavior {
	public ZoomingReferenceBehavior(Penner penner) {
		super(penner);
	}
	int startx = this.penner.x;
	int starty = this.penner.y;
	@Override public void start() {}
	@Override public void onTock() {}
	@Override
	public void onMove() {
		this.penner.workspace.getReferenceManager().zoomTransform(
				(float)Math.pow(1.0015, 1+(this.penner.rawY - this.penner.oldRawY)), startx, starty);
	}
}