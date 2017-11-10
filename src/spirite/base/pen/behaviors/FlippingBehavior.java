package spirite.base.pen.behaviors;

import spirite.base.pen.Penner;
import spirite.base.util.MUtil;

public class FlippingBehavior extends StateBehavior {
	public FlippingBehavior(Penner penner) {
		super(penner);
	}
	int startX, startY;
	
	@Override
	public void start() {
		startX = this.penner.x;
		startY = this.penner.y;
	}
	@Override
	public void onMove() {
		
	}
	@Override
	public void onPenUp() {
		boolean horizontal = MUtil.distance(this.penner.x , this.penner.y, startX, startY) < 5 
				||Math.abs(this.penner.x - startX) > Math.abs(this.penner.y - startY);
		this.penner.tryFlip( horizontal);
		
		super.onPenUp();
	}
	@Override public void onTock() {}
	
	
}