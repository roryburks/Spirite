package spirite.base.graphics.awt;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.RawImage;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;

/**
 * AWTContext is a GraphicsContext using only native AWT calls (as well as some
 * software rendering).  For the most part it is a wrapper for a Graphics object.
 * 
 * @author Rory Burks
 *
 */
public class AWTContext extends GraphicsContext{
	
	private final Graphics2D g2;
	private final int width, height;
	
	public static AffineTransform toAT( MatTrans trans) {
		return new AffineTransform(
				trans.getM00(), trans.getM10(), trans.getM01(),
				trans.getM11(), trans.getM02(), trans.getM12());
	}
	public static MatTrans toMT( AffineTransform trans) {
		return new MatTrans(
				(float)trans.getScaleX(), (float)trans.getShearX(), (float)trans.getTranslateX(),
				(float)trans.getShearY(), (float)trans.getScaleY(), (float)trans.getTranslateY());
	}
	
	public AWTContext( Graphics g, int width, int height) {
		this.g2 = (Graphics2D)g;
		this.width = width;
		this.height = height;
	}
	
	public AWTContext(BufferedImage bi) {
		this.g2 = (Graphics2D)bi.getGraphics();
		this.width = bi.getWidth();
		this.height = bi.getHeight();
	}
	public Graphics getGraphics() {return g2;}

	@Override
	public void drawBounds(RawImage mask, int c) {
		Rectangle r = g2.getClipBounds();
		
		AffineTransform old = g2.getTransform();
		g2.setTransform(new AffineTransform());
		
		BufferedImage bi = new BufferedImage( r.width, r.height, HybridHelper.BI_FORMAT);
		Graphics2D g2BI = (Graphics2D)bi.getGraphics();
		g2BI.setTransform(old);
		GraphicsContext gc = new AWTContext( g2BI, bi.getWidth(), bi.getHeight());
		gc.setComposite( Composite.SRC_OVER, 0.5f);
		gc.drawImage(mask, 1, 0);
		gc.drawImage(mask, -1, 0);
		gc.drawImage(mask, 0, 1);
		gc.drawImage(mask, 0, -1);
		gc.setComposite( Composite.DST_OUT, 1);
		gc.drawImage(mask, 0, 0);
		g2BI.dispose();
		
		g2.drawImage(bi, 0, 0, null);
		
		g2.setTransform(old);
	}

	@Override
	public void clear() {
		java.awt.Composite comp = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		g2.fillRect( 0, 0, width, height);
		g2.setComposite(comp);
	}

	// ==========
	// ==== Transform Methods
	@Override public void setTransform(MatTrans trans) { g2.setTransform( toAT(trans)); }
	@Override public MatTrans getTransform() { return toMT(g2.getTransform()); }
	@Override public void translate(double offsetX, double offsetY) {g2.translate(offsetX, offsetY);}	
	@Override public void scale(double sx, double sy) { g2.scale(sx, sy);}
	@Override public void transform(MatTrans trans) {g2.transform( toAT(trans)); }

	@Override public void setColor(int color) {g2.setColor(new Color(color,true));}
	@Override
	public void setComposite(Composite composite, float alpha) {
		int i = AlphaComposite.SRC_OVER;
		
		switch( composite) {
		case SRC: i = AlphaComposite.SRC; break;
		case SRC_IN: i = AlphaComposite.SRC_IN; break;
		case SRC_OVER: i = AlphaComposite.SRC_OVER; break;
		case SRC_ATOP: i = AlphaComposite.SRC_ATOP; break;
		case SRC_OUT: i = AlphaComposite.SRC_OUT; break;
		case DST: i = AlphaComposite.DST; break;
		case DST_ATOP: i = AlphaComposite.DST_ATOP; break;
		case DST_OUT: i = AlphaComposite.DST_OUT; break;
		case DST_IN: i = AlphaComposite.DST_IN; break;
		case DST_OVER:i = AlphaComposite.DST_OVER; break;
		case CLEAR: i = AlphaComposite.CLEAR; break;
		case XOR: i = AlphaComposite.XOR; break;
		}
		g2.setComposite( AlphaComposite.getInstance(i, alpha));
	}
	@Override
	public float getAlpha() {
		if( g2.getComposite() instanceof AlphaComposite)
			return ((AlphaComposite)g2.getComposite()).getAlpha();
		return 1.0f;
	}
	@Override
	public Composite getComposite() {
		switch(((AlphaComposite) g2.getComposite()).getRule()) {
		case AlphaComposite.SRC: return Composite.SRC;
		case AlphaComposite.SRC_OUT: return Composite.SRC_OUT;
		case AlphaComposite.SRC_OVER: return Composite.SRC_OVER;
		case AlphaComposite.SRC_ATOP: return Composite.SRC_ATOP;
		case AlphaComposite.SRC_IN: return Composite.SRC_IN;
		case AlphaComposite.DST: return Composite.DST;
		case AlphaComposite.DST_OUT: return Composite.DST_OUT;
		case AlphaComposite.DST_OVER: return Composite.DST_OVER;
		case AlphaComposite.DST_ATOP: return Composite.DST_ATOP;
		case AlphaComposite.DST_IN: return Composite.DST_IN;
		case AlphaComposite.XOR: return Composite.XOR;
		case AlphaComposite.CLEAR: return Composite.CLEAR;
		}
		return null;
	}
	
	// =========
	// ==== LineAttributes
	@Override
	public void setLineAttributes(LineAttributes line) {
		g2.setStroke(new BasicStroke( line.width, LAtoBSCap(line.cap), 
				LAtoBSJoin(line.join), 10.0f, line.dashes, 0.0f));
	}
	@Override
	public LineAttributes getLineAttributes() {
		if( g2.getStroke() instanceof BasicStroke) {
			BasicStroke bs = (BasicStroke)g2.getStroke();
			return new LineAttributes( bs.getLineWidth(), BStoLACap(bs.getEndCap()), 
					BStoLAJoin(bs.getLineJoin()), bs.getDashArray());
		}
		return null;
	}
	private int LAtoBSCap(CapMethod cap) {
		switch( cap) {
			case ROUND: return BasicStroke.CAP_ROUND; 
			case SQUARE: return BasicStroke.CAP_SQUARE;
			case NONE: 
			default:
				return BasicStroke.CAP_BUTT;
		}
	}
	private int LAtoBSJoin( JoinMethod join) {
		switch( join) {
			case BEVEL: return BasicStroke.JOIN_BEVEL;
			case MITER: return BasicStroke.JOIN_MITER;
			case ROUNDED:
			default:
				return BasicStroke.JOIN_ROUND;
		}
	}
	private CapMethod BStoLACap(int cap) {
		switch(cap) {
			case BasicStroke.CAP_ROUND: return CapMethod.ROUND;
			case BasicStroke.CAP_SQUARE: return CapMethod.SQUARE;
			case BasicStroke.CAP_BUTT:
			default:
				return CapMethod.NONE;
		}
	}
	private JoinMethod BStoLAJoin( int join) {
		switch(join) {
			case BasicStroke.JOIN_BEVEL: return JoinMethod.BEVEL;
			case BasicStroke.JOIN_MITER: return JoinMethod.MITER;
			case BasicStroke.JOIN_ROUND:
			default: 
				return JoinMethod.ROUNDED;
		}
	}
	
	@Override public void drawRect(int x, int y, int w, int h) { g2.drawRect(x, y, w, h);}
	@Override public void drawOval(int x, int y, int w, int h) { g2.drawOval(x,y,w,h);}
	@Override public void drawPolyLine(int[] x, int[] y, int count) {g2.drawPolyline(x, y, count); }
	@Override public void drawLine(int x1, int y1, int x2, int y2) {g2.drawLine(x1, y1, x2, y2);}
	@Override public void draw(Shape shape) {g2.draw(shape);}

	@Override public void fillRect(int x, int y, int w, int h) {g2.fillRect(x, y, w, h);}
	@Override public void fillOval(int x, int y, int w, int h) {g2.fillOval(x, y, w, h);}
	@Override public void fillPolygon(int[] x, int[] y, int count) {g2.fillPolygon(x, y, count);}

	@Override public void drawImage(RawImage img, int x, int y) {
		if( img instanceof ImageBI)
			g2.drawImage( ((ImageBI)img).img,  x,  y, null);
		else
			MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unsupported Image Type");
	}
	@Override public void drawHandle(ImageHandle handle, int x, int y) { drawImage( handle.deepAccess(), x, y); }
}
