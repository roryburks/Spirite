package spirite.base.pen.behaviors;

import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.Penner;

public class MovingSelectionBehavior extends StateBehavior {
	public MovingSelectionBehavior(Penner penner) {
		super(penner);
	}
	@Override public void start() {}
	@Override public void onTock() {}
	@Override
	public void onMove() {
		SelectionMask selection = this.penner.selectionEngine.getSelection();
		if( selection == null) 
			end();
		else if( this.penner.oldX != this.penner.x || this.penner.oldY != this.penner.y) {
			this.penner.selectionEngine.setOffset( 
					selection.getOX() + (this.penner.x - this.penner.oldX),
					selection.getOY() + (this.penner.y - this.penner.oldY));
		}
	}
}