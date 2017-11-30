package spirite.base.pen.behaviors;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.pen.Penner;
import spirite.base.util.Colors;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.MatTrans.NoninvertableException;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;

abstract class TransformBehavior extends DrawnStateBehavior {
	
		enum TransormStates {
			READY, ROTATE, RESIZE, MOVING, INACTIVE
		}
	
		TransformBehavior(Penner penner) {
			super(penner);
		}
		private TransormStates state = TransormStates.READY;
		
		// The Calculation Transform is a version of the Locked Transform which has
		//	all the relevent offsets built-in so that calculation changes in mouse
		//	movement with respect to the selection's center can be easily performed.
		private MatTrans calcTrans = new MatTrans();
		
		private int startX, startY;
		
		private float oldScaleX, oldScaleY;
		private float oldRot;
		
		protected int overlap = -1;
		
		protected float scaleX, scaleY;
		protected float translateX, translateY;
		protected float rotation;
		protected Rect region;

		protected abstract void onScaleChanged();
		protected abstract void onTrnalsationChanged();
		protected abstract void onRotationChanged();
		
		private void _internetSetScale( float scaleX, float scaleY) {
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			onScaleChanged();
		}
		private void _internalSetTranslation( float transX, float transY) {
			translateX = transX;
			translateY = transY;
			onTrnalsationChanged();
		}
		private void _internalSetRotion( float rotation) {
			this.rotation = rotation;
			onRotationChanged();
		}
		
		
		
		// 0123 : NESW
		// 4567 : NW NE SE SW
		// 89AB : NW NE SE SW (rotation)
		// C : Moving
		
		protected MatTrans getWorkingTransform() {
			MatTrans wTrans = new MatTrans();

			wTrans.preScale(scaleX, scaleY);
			wTrans.preRotate(rotation);
			wTrans.preTranslate( translateX, translateY);
			return wTrans;
		}
		
		protected MatTrans calcDisplayTransform() {
			float zoom = this.penner.view.getZoom();
			MatTrans relTrans = new MatTrans();
			relTrans.preTranslate(-region.width/2, -region.height/2);
			relTrans.preConcatenate(getWorkingTransform());
			relTrans.preTranslate(region.width/2+region.x, region.height/2+region.y);
			relTrans.preScale(zoom, zoom);
			relTrans.preTranslate(this.penner.view.itsX(0), this.penner.view.itsY(0));
			
			return relTrans;
		}

		
		@Override
		public void paintOverlay(GraphicsContext gc) {
			if( region == null || region.isEmpty() || state == TransormStates.INACTIVE)
				return;
			
			float zoom = this.penner.view.getZoom();
			
			MatTrans origTrans = gc.getTransform();
			MatTrans relTrans = calcDisplayTransform();
			
			gc.setTransform(relTrans);

			int w = region.width;
			int h = region.height;
			gc.setColor(Colors.BLACK);
			gc.drawRect( 0, 0, w, h);
			
//			Stroke defStroke = new BasicStroke( 2/zoom);
			gc.setColor(Colors.GRAY);
//			gc.setStroke(defStroke);
			
			Vec2 p = null;
			try {
				p = relTrans.inverseTransform(new Vec2(this.penner.rawX,this.penner.rawY));
			} catch (NoninvertableException e) {
				e.printStackTrace();
			}
			
			float sw = w*0.3f;	// Width of corner rect
			float sh = h*0.3f;	// Height
			float x2 = w*0.7f;	// Offset of right rect
			float y2 = h*0.7f;	// " bottom
			float di = Math.max(h*0.2f, 10);	// Diameter of rotate thing
			float of = h*0.25f*0.2f;

			float b =1/zoom;
			
			List<Shape> s = new ArrayList<>(12);
			s.add(new Rectangle2D.Float(sw+b, b, x2-sw-b*2, sh-b*2));	// N
			s.add(new Rectangle2D.Float(x2+b, sh+b, sw-b*2, y2-sh-b*2));// E
			s.add(new Rectangle2D.Float(sw+b, y2+b, x2-sw-b*2, sh-b*2));// S
			s.add(new Rectangle2D.Float(0+b, sh+b, sw-b*2, y2-sh-b*2));	// W
			
			s.add(new Rectangle2D.Float(b, b, sw-b*2, sh-b*2));			// NW
			s.add(new Rectangle2D.Float(x2+b, b, sw-b*2, sh-b*2));		// NE
			s.add(new Rectangle2D.Float(x2+b, y2+b, sw-b*2, sh-b*2));	// SE
			s.add(new Rectangle2D.Float(b, y2+b, sw-b*2, sh-b*2));		// SW

			s.add(new Ellipse2D.Float( -di+of, -di+of, di, di));	// NW
			s.add(new Ellipse2D.Float( w-of, -di+of, di, di));	// NE
			s.add(new Ellipse2D.Float( w-of, h-of, di, di));	// SE
			s.add(new Ellipse2D.Float( -di+of, h-of, di, di));	// SW

			s.add(new Rectangle2D.Float(sw+b, sh+b, x2-sw-b*2, y2-sh-b*2));	// Center

			gc.setComposite(gc.getComposite(), 0.5f);
			if( this.state == TransormStates.READY)
				overlap = -1;
			for( int i=0; i<s.size(); ++i) {
				Shape shape = s.get(i);
				if( overlap == i || (overlap == -1 && shape.contains( new Point2D.Float(p.x, p.y)))) {
					gc.setColor(Colors.YELLOW);
//					gc.setStroke(new BasicStroke( 4/zoom));
					gc.draw(shape);
					gc.setColor(Colors.GRAY);
//					gc.setStroke(defStroke);
					overlap = i;
				}
				else gc.draw(shape);
			}
			gc.setComposite(gc.getComposite(), 1f);

			
			gc.setTransform(origTrans);
		}

		@Override
		public void onMove() {
			switch( this.state) {
			case MOVING:
				
				if( this.penner.oldX != this.penner.x || this.penner.oldY != this.penner.y) {
					_internalSetTranslation( translateX + this.penner.x - this.penner.oldX, translateY + this.penner.y - this.penner.oldY );
				}
				break;
			case READY:
				break;
			case RESIZE:{
				Vec2 pn = calcTrans.transform(new Vec2(this.penner.rawX,this.penner.rawY));
				Vec2 ps = calcTrans.transform(new Vec2(startX,startY));

				float sx = (overlap == 0 || overlap == 2) ? scaleX : pn.x/ps.x * oldScaleX;
				float sy = (overlap == 1 || overlap == 3) ? scaleY : pn.y/ps.y * oldScaleY;
				
				_internetSetScale(sx, sy);
				break;}
			case ROTATE:{
				Vec2 pn = calcTrans.transform(new Vec2(this.penner.rawX,this.penner.rawY));
				Vec2 ps = calcTrans.transform(new Vec2(startX,startY));

				
				
				

				double start = Math.atan2(ps.y, ps.x);
				double end =  Math.atan2(pn.y, pn.x);
				
				_internalSetRotion((float)(end-start + oldRot));
				break;}
			default:
				break;
			}
			// TODO
			//selectionEngine.proposeTransform(getWorkingTransform());
		}

		public TransormStates getState() {return state;}
		protected void setState( TransormStates newState) {
			switch( newState) {
			case RESIZE:
				startX = this.penner.rawX;
				startY = this.penner.rawY;
				
				calcTrans = calcDisplayTransform();
				oldScaleX = scaleX;
				oldScaleY = scaleY;
				try {
					calcTrans = calcTrans.createInverse();
				} catch (NoninvertableException e) {
					e.printStackTrace();
				}
				break;
			case ROTATE:
				startX = this.penner.rawX;
				startY = this.penner.rawY;

				calcTrans = calcDisplayTransform();
				oldRot = rotation;
				this.state = TransormStates.ROTATE;
				try {
					calcTrans = calcTrans.createInverse();
				} catch (NoninvertableException e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}

			this.state = newState;
		}
	}