package spirite.base.pen.behaviors;

import spirite.base.image_data.GroupTree.Node;
import spirite.base.pen.Penner;

public class MovingNodeBehavior extends StateBehavior {
	final Node node;
	
	public MovingNodeBehavior( Penner penner, Node node) {
		super(penner);
		this.node = node;
	}
	@Override public void start() {}
	@Override public void onTock() {}
	@Override
	public void onMove() {
		if( node != null && (this.penner.oldX != this.penner.x || this.penner.oldY != this.penner.y))
			node.setOffset( node.getOffsetX() + (this.penner.x - this.penner.oldX), 
							 node.getOffsetY() + (this.penner.y - this.penner.oldY));
	}
}