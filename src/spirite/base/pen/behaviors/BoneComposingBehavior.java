package spirite.base.pen.behaviors;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.CapMethod;
import spirite.base.graphics.GraphicsContext.JoinMethod;
import spirite.base.graphics.GraphicsContext.LineAttributes;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IPuppetBoneDrawer;
import spirite.base.pen.Penner;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;

public class BoneComposingBehavior extends DrawnStateBehavior  {
	private final IPuppetBoneDrawer drawer;
	
	float startX, startY;
	
	private enum State {
		MOVING_MIDDLE,
		MOVING_1,
		MOVING_2,
		MAKING
	}
	private State state;
	BaseBone bone;

	public BoneComposingBehavior(Penner penner, IPuppetBoneDrawer drawer) {
		super(penner);
		
		this.drawer = drawer;
	}

	@Override
	public void paintOverlay(GraphicsContext gc) {
		gc.setTransform(penner.view.getViewTransform());
		gc.setColor(Colors.CYAN);
		gc.setLineAttributes(new LineAttributes(3, CapMethod.ROUND, JoinMethod.MITER));
		
		switch( state) {
		case MAKING:
			gc.drawLine(startX, startY, penner.x, penner.y);
			break;
		case MOVING_1:
			gc.drawLine(penner.x, penner.y, bone.x2, bone.y2);
			break;
		case MOVING_2:
			gc.drawLine(bone.x1, bone.y1, penner.x, penner.y);
			break;
		case MOVING_MIDDLE:
			gc.drawLine( bone.x1 + penner.x - startX, 
					bone.y1 + penner.y - startY,
					bone.x2 + penner.x - startX, 
					bone.y2 + penner.y - startY);
			break;
		}
	}

	@Override
	public void start() {
		float WIDTH = 10;
		
		bone = drawer.grabBone(penner.x, penner.y, WIDTH);

		startX = penner.x;
		startY = penner.y;
		
		if( bone == null)
			state = State.MAKING;
		else {
			double d1 = MUtil.distance(bone.x1, bone.y1, penner.x, penner.y);
			double d2 = MUtil.distance(bone.x2, bone.y2, penner.x, penner.y);
			if( d1 < d2 && d1 < WIDTH)
				state = State.MOVING_1;
			else if( d2 < WIDTH )
				state = State.MOVING_2;
			else 
				state = State.MOVING_MIDDLE;
		}
	}

	@Override public void onTock() {}
	@Override public void onMove() {}
	@Override
	public void onPenUp() {
		switch(state) {
		case MAKING:
			if( startX != penner.x || startY != penner.y)
				drawer.makeBone(startX, startY, penner.x, penner.y);
			break;
		case MOVING_1:
			drawer.makeBone(penner.x, penner.y, bone.x2, bone.y2);
			break;
		case MOVING_2:
			drawer.makeBone(bone.x1, bone.y1, penner.x, penner.y);
			break;
		case MOVING_MIDDLE:
			drawer.makeBone( bone.x1 + penner.x - startX, 
					bone.y1 + penner.y - startY,
					bone.x2 + penner.x - startX, 
					bone.y2 + penner.y - startY);
		
		}
		
		super.onPenUp();
	}

}
