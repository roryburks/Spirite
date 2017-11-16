package spirite.base.pen.behaviors;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.FloatControl;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.CapMethod;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.GraphicsContext.JoinMethod;
import spirite.base.graphics.GraphicsContext.LineAttributes;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IBoneDrawer;
import spirite.base.pen.Penner;
import spirite.base.util.Colors;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;

public class BoneContortionBehavior extends DrawnStateBehavior {
	private enum State {
		MAKING,
		BETWEEN,
		DEFORMING,
		PROPOSING
	}
	State state = State.MAKING;
	
	float x1, y1, x2, y2;
	FloatCompactor fx, fy;
	private final IBoneDrawer drawer;
	CubicSplineInterpolator2D proposing;
	List<Vec2> proposedPoints;
	
	public BoneContortionBehavior(Penner penner, IBoneDrawer drawer) {
		super(penner);
		this.drawer = drawer;
	}

	@Override
	public void paintOverlay(GraphicsContext gc) {
		//gc.pushTransform();
		
		
		gc.transform(penner.view.getViewTransform());
		gc.setColor(Colors.GRAY);
//		
//		Vec2 n = new Vec2((x2+x1)/2, (y2+y1)/2);
//		Vec2 aft = (new Vec2(y1-y2, x2-x1)).normalize().scalar(5);
//		float[] x = new float[] {
//			x1, n.x + aft.x, x2, n.x - aft.x
//		};
//		float[] y = new float[] {
//			y1, n.y + aft.y,y2, n.y - aft.y
//		};
//		gc.drawPolyLine(x, y, 4);
		
		gc.setComposite(Composite.SRC_OVER, 1.0f);
		gc.setColor(Colors.WHITE);
		gc.setLineAttributes(new LineAttributes(3, CapMethod.SQUARE, JoinMethod.MITER));
		gc.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
		
		
		if( state == State.DEFORMING) {
			gc.setColor(Colors.BLACK);
			gc.drawPolyLine(fx.toArray(), fy.toArray(), fx.size());
		}
		if( state == State.PROPOSING) {
			gc.setColor(Colors.BLACK);

			FloatCompactor dx = new FloatCompactor();
			FloatCompactor dy = new FloatCompactor();
			for( float t=- proposing.getCurveLength(); t < proposing.getCurveLength()*3f/2f; t += 5) {
				Vec2 dp = proposing.eval(t);
				dx.add(dp.x);
				dy.add(dp.y);
			}
//			Vec2 dp = (proposing.eval(proposing.getCurveLength()));
//			dx.add(dp.x);
//			dy.add(dp.y);
			gc.drawPolyLine(dx.toArray(), dy.toArray(), dx.size());

			gc.setColor(Colors.RED);
			gc.setLineAttributes(new LineAttributes(1, CapMethod.SQUARE, JoinMethod.MITER));
			for( Vec2 point : proposedPoints)
				gc.drawOval((int)point.x-2, (int)point.y-2, 4, 4);
		}
		//gc.popTransform();
	}

	@Override
	public void start() {
		x1 = penner.x;
		y1 = penner.y;
	}

	@Override
	public void onTock() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onPenUp() {
		if( state == State.MAKING) {
			state = State.BETWEEN;
			fx = new FloatCompactor();
			fy = new FloatCompactor();
		}
		if( state == State.DEFORMING) {
			BaseBone b = new BaseBone(x1, y1, x2, y2, 1);
			
			CubicSplineInterpolator2D prel = new CubicSplineInterpolator2D(fx.toArray(), fy.toArray(), false);
			
			float clen = prel.getCurveLength();
			proposedPoints = new ArrayList<>();
			for( int i=0; i < 4; ++i) {
				proposedPoints.add(prel.eval(i*clen/3));
			}
			proposing = new CubicSplineInterpolator2D(proposedPoints, true);
			proposing.setExtrapolating(true);
			
			state = State.PROPOSING;
			drawer.contort(b, proposing);
			
			end();
		}
	}
	@Override
	public void onPenDown() {
		if( state == State.BETWEEN) {
			state = State.DEFORMING;
		}
	}

	@Override
	public void onMove() {
		if( state == State.MAKING) {
			x2 = penner.x;
			y2 = penner.y;
		}
		else if( state == State.DEFORMING){
			fx.add(penner.x);
			fy.add(penner.y);
		}
	}

}
