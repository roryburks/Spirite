package spirite.graphics.awt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import spirite.Globals;
import spirite.graphics.GraphicsContext;

/**
 * AWTContext is a GraphicsContext using only native AWT calls (as well as some
 * software rendering).  For the most part it is a wrapper for a Graphics object.
 * 
 * @author Rory Burks
 *
 */
public class AWTContext extends GraphicsContext{
	
	private final Graphics2D g2;
	
	public AWTContext( Graphics g) {
		this.g2 = (Graphics2D)g;
	}
	
	public Graphics getGraphics() {return g2;}

	@Override
	public void drawBounds(BufferedImage mask, int c) {
		Rectangle r = g2.getClipBounds();
		
		AffineTransform old = g2.getTransform();
		g2.setTransform(new AffineTransform());
		
		BufferedImage bi = new BufferedImage( r.width, r.height, Globals.BI_FORMAT);
		Graphics2D g2BI = (Graphics2D)(bi.getGraphics());
		g2BI.setTransform(old);
		g2BI.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g2BI.drawImage(mask, 1, 0, null);
		g2BI.drawImage(mask, -1, 0, null);
		g2BI.drawImage(mask, 0, 1, null);
		g2BI.drawImage(mask, 0, -1, null);
		g2BI.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_OUT, 1));
		g2BI.drawImage(mask, 0, 0, null);
		g2BI.dispose();
		
		g2.drawImage(bi, 0, 0, null);
		
		g2.setTransform(old);
	}

	@Override
	public void clear() {
		Rectangle r = g2.getClipBounds();
		g2.clearRect(r.x, r.y, r.width, r.height);
	}

	@Override public void setTransform(AffineTransform trans) { g2.setTransform(trans); }
	@Override public AffineTransform getTransform() { return g2.getTransform(); }

	@Override public void setColor(Color color) {g2.setColor(color);}
	@Override
	public void setComposite(Composite composite, float alpha) {
		int i = AlphaComposite.SRC_OVER;
		
		switch( composite) {
		case SRC_OVER: i = AlphaComposite.SRC_OVER; break;
		}
		g2.setComposite( AlphaComposite.getInstance(i, alpha));
		
	}
	
	@Override public void drawRect(int x, int y, int w, int h) { g2.drawRect(x, y, w, h);}
	@Override public void drawOval(int x, int y, int w, int h) { g2.drawOval(x,y,w,h);}
	@Override public void drawPolyLine(int[] x, int[] y, int count) {g2.drawPolyline(x, y, count); }
	@Override public void drawLine(int x1, int y1, int x2, int y2) {g2.drawLine(x1, y1, x2, y2);}
	@Override public void draw(Shape shape) {g2.draw(shape);}

	@Override public void fillRect(int x, int y, int w, int h) {g2.fillRect(x, y, w, h);}
	@Override public void fillOval(int x, int y, int w, int h) {g2.fillOval(x, y, w, h);}



}
