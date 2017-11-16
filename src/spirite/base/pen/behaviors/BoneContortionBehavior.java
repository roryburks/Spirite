package spirite.base.pen.behaviors;

import java.util.ArrayList;
import java.util.List;

import spirite.base.brains.ToolsetManager.BoneStretchMode;
import spirite.base.brains.ToolsetManager.MToolsetObserver;
import spirite.base.brains.ToolsetManager.Property;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.CapMethod;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.GraphicsContext.JoinMethod;
import spirite.base.graphics.GraphicsContext.LineAttributes;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IBoneDrawer;
import spirite.base.pen.Penner;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;
import spirite.hybrid.HybridHelper;

public class BoneContortionBehavior extends DrawnStateBehavior 
	implements MToolsetObserver 
{
	private enum State {
		MAKING,
		BETWEEN,
		DEFORMING,
		PROPOSING,
		MOVING_PROP,
		
	}
	State state = State.MAKING;
	
	float x1, y1, x2, y2;
	int startX, startY;
	
	FloatCompactor fx, fy;
	private final IBoneDrawer drawer;
	CubicSplineInterpolator2D proposing;
	List<Vec2> proposedPoints;
	CubicSplineInterpolator2D memoryProposition;
	List<Vec2> memoryProposedPoints;
	
	public BoneContortionBehavior(Penner penner, IBoneDrawer drawer) {
		super(penner);
		this.drawer = drawer;
		penner.toolsetManager.addToolsetObserver(this);
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
		if( proposing != null) {
			gc.setColor(Colors.BLACK);

			FloatCompactor dx = new FloatCompactor();
			FloatCompactor dy = new FloatCompactor();
			for( float t= 0; t < proposing.getCurveLength(); t += 5) {
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
	public void end() {
		HybridHelper.queueToRun(() -> penner.toolsetManager.removeToolsetObserver(this));
		super.end();
	}

	@Override public void onTock() {}
	@Override
	public void onPenUp() {
		if( state == State.MAKING) {
			state = State.BETWEEN;
			fx = new FloatCompactor();
			fy = new FloatCompactor();
		}
		else if( state == State.DEFORMING) {
			
			CubicSplineInterpolator2D prel = new CubicSplineInterpolator2D(fx.toArray(), fy.toArray(), false);
			
			float clen = prel.getCurveLength();
			proposedPoints = new ArrayList<>();
			for( int i=0; i < SUBDIVISIONS+1; ++i) {
				proposedPoints.add(prel.eval(i*clen/(SUBDIVISIONS)));
			}
			proposing = new CubicSplineInterpolator2D(proposedPoints, true);
			proposing.setExtrapolating(true);
			
			state = State.PROPOSING;
			//BaseBone b = new BaseBone(x1, y1, x2, y2, 1);
			//drawer.contort(b, proposing);			
			//end();
		}
		else if( state == State.MOVING_PROP) {
			if( memoryProposedPoints != null)  {
				for( Vec2 p : memoryProposedPoints) {
					p.x += penner.x - startX;
					p.y += penner.y - startY;
				}
				memoryProposition = new CubicSplineInterpolator2D(memoryProposedPoints, false);
				memoryProposition.setExtrapolating(true);
			}
			
			state = State.PROPOSING;
		}
		penner.repaint();
	}
	@Override
	public void onPenDown() {
		if( state == State.BETWEEN) {
			state = State.DEFORMING;
		}
		else if( state == State.PROPOSING) {
			if( penner.holdingShift) {
				startX = penner.x;
				startY = penner.y;
				state = State.MOVING_PROP;
			}
			else {
				state = State.DEFORMING;
				fx = new FloatCompactor();
				fy = new FloatCompactor();
				proposing = null;
				proposedPoints = null;
				memoryProposition = null;
				memoryProposedPoints = null;
			}
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
		else if( state == State.MOVING_PROP) {
			for( Vec2 p : proposedPoints) {
				p.x += penner.x - penner.oldX;
				p.y += penner.y - penner.oldY;
			}
			proposing = new CubicSplineInterpolator2D(proposedPoints, false);
			proposing.setExtrapolating(true);
		}
	}

	// :: MToolsetObserver
	@Override public void toolsetChanged(Tool newTool) {}
	@Override
	public void toolsetPropertyChanged(Tool tool, Property property) {
		if( tool != Tool.BONE)
			return;
		
		ToolSettings settings = penner.toolsetManager.getToolSettings(Tool.BONE);
		
		if( property.getId().equals("resize") ) {
			if( state == State.PROPOSING) {
				if( memoryProposition == null) {
					memoryProposedPoints = proposedPoints;
					memoryProposition = proposing;
				}
				
				_doScale((BoneStretchMode)settings.getProperty("mode").getValue(), 
						(Float)settings.getProperty("leniency").getValue());
			}
			else HybridHelper.beep();
		}
		if((property.getId().equals("leniency") || property.getId().equals("mode")) && memoryProposition != null) {
			_doScale((BoneStretchMode)settings.getProperty("mode").getValue(), 
					(Float)settings.getProperty("leniency").getValue());
		}
		if( property.getId().equals("do")) {
			if( proposing != null)
				drawer.contort(new BaseBone(x1, y1, x2, y2, 1), proposing);
			end();
		}
	}
	
	private void _doScale( BoneStretchMode mode, float leniency) {
		switch( mode) {
		case CLIP_EVEN:{
			// Note: 80% code duplicated from CLIP_HEAD
			float bone_len = (float)MUtil.distance(x1, y1, x2, y2);
			float curve_len = memoryProposition.getCurveLength();
			
			if( bone_len * (1-leniency) > curve_len)
				bone_len = bone_len * (1-leniency);
			else if( bone_len*(1 + leniency) > curve_len) {
				proposing = memoryProposition;
				break;
			}
			else
				bone_len = bone_len * (1+leniency);

			proposedPoints = new ArrayList<>();
			float start = (curve_len - bone_len)/2f;
			float end = curve_len - start;
			for( int i=0; i < SUBDIVISIONS+1; ++i) {
				proposedPoints.add(memoryProposition.eval( start + i * (end - start) / (float)SUBDIVISIONS));
			}
			proposing = new CubicSplineInterpolator2D(proposedPoints, false);
			proposing.setExtrapolating(true);
			break;}
		case SCALE:{
			float bone_len = (float)MUtil.distance(x1, y1, x2, y2);
			float curve_len = memoryProposition.getCurveLength();

			if( bone_len * (1-leniency) > curve_len)
				bone_len = bone_len * (1-leniency);
			else if( bone_len*(1 + leniency) > curve_len) {
				proposing = memoryProposition;
				break;
			}
			else
				bone_len = bone_len * (1+leniency);
			float scale = bone_len / curve_len;

			Rect rect = MUtil.rectFromPoints(memoryProposedPoints);
			Vec2 center = new Vec2( rect.x + rect.width/2, rect.y + rect.height/2);
			
			MatTrans trans = new MatTrans();
			trans.preTranslate(-center.x, -center.y);
			trans.preScale(scale, scale);
			trans.preTranslate(center.x, center.y);
			
			proposedPoints = new ArrayList<>();
			for( Vec2 p : memoryProposedPoints) {
				proposedPoints.add( trans.transform(p, new Vec2()));
			}
			proposing = new CubicSplineInterpolator2D(proposedPoints, false);
			proposing.setExtrapolating(true);
			break;}
		case SCALE_TO_BONE:
			break;
		case CLIP_HEAD: {
			float bone_len = (float)MUtil.distance(x1, y1, x2, y2);
			float curve_len = memoryProposition.getCurveLength();
			
			if( bone_len * (1-leniency) > curve_len)
				bone_len = bone_len * (1-leniency);
			else if( bone_len*(1 + leniency) > curve_len) {
				proposing = memoryProposition;
				break;
			}
			else
				bone_len = bone_len * (1+leniency);
			
			proposedPoints = new ArrayList<>();
			for( int i=0; i < SUBDIVISIONS+1; ++i) {
				proposedPoints.add(memoryProposition.eval(i * bone_len / (float)SUBDIVISIONS));
			}
			proposing = new CubicSplineInterpolator2D(proposedPoints, false);
			proposing.setExtrapolating(true);
			break;}
		case INTELI: {
			break;}
		}

		penner.repaint();
	}
	
	final int SUBDIVISIONS = 4;

}
