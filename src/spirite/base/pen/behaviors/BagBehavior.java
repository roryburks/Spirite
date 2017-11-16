package spirite.base.pen.behaviors;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.pen.Penner;
import spirite.base.util.Colors;
import spirite.base.util.glmath.Vec2;

public class BagBehavior extends DrawnStateBehavior {
	float startX, startY;
	float cX, cY;
	float springConstant = 0.05f;
	
	public BagBehavior( Penner penner) {
		super(penner);
	}
	@Override
	public void start() {
		startX = penner.x;
		startY = penner.y;
		cX = startX;
		cY = startY;
	}
	
	@Override
	public void onPenUp() {
	}
	
	@Override
	public void paintOverlay(GraphicsContext gc) {
		cY += 0.8f;
		Vec2 counter=  new Vec2(cX - startX, cY-startY).scalar(-springConstant);
		cX += counter.x;
		cY += counter.y;
		gc.setColor(Colors.BLACK);
		gc.setComposite(Composite.SRC_OVER, 1);
		gc.setTransform(penner.view.getViewTransform());
		gc.drawOval((int)startX-3, (int)startY-3, 6, 6);
		gc.drawOval((int)cX-3, (int)cY-3, 6, 6);
	}



	@Override
	public void onTock() {
//		cY += 0.2f;
//		Vec2 counter=  new Vec2(cX - startX, cY-startY).scalar(-springConstant);
//		cX += counter.x;
//		cY += counter.y;
	}

	@Override
	public void onMove() {
		// TODO Auto-generated method stub

	}

}
