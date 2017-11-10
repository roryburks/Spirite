package spirite.base.pen.behaviors;

import spirite.base.graphics.GraphicsContext;
import spirite.base.pen.Penner;

public abstract class DrawnStateBehavior extends StateBehavior {
	public DrawnStateBehavior(Penner penner) {
		super(penner);
	}
	
	@Override
	public void end() {
		super.end();
		this.penner.context.repaint();
	}
	public abstract void paintOverlay( GraphicsContext gc);
}