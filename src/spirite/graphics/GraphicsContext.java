package spirite.graphics;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * GraphicsContext is an abstract class which wraps all graphical functionality 
 * which can be rendered by different native engines (e.g. either OpenGL or the
 * basic Java AWT methods) depending on settings/device capability.
 * 
 * @author Rory Burks
 *
 */
public abstract class GraphicsContext {
	// TODO: the parameters and return value of these methods are expected to change
	//	as the GraphicsContext works its way into the UI
	
	/**
	 * Draws a border around the given image with the given transform applied to 
	 * it, returns a BufferedImage of dimensions (swidth x sheight).
	 * @param image	The image to draw a border around.
	 * @param cycle	The offset of the cycle in which the dashes are to be drawn
	 * @param trans	The transform to apply to the image to draw it on the screen
	 * @param swidth	The width of the screen to draw it on.
	 * @param sheight	The height of the screen to draw it on.
	 * @return
	 */
	public abstract void drawBounds(BufferedImage mask, int c);

	public abstract void clear();
	
	public abstract void setTransform( AffineTransform trans);
	public abstract AffineTransform getTransform();
	public abstract void setColor(Color color);
	
	public enum Composite {SRC_OVER};
	public abstract void setComposite( Composite composite, float alpha);

	public abstract void drawRect(int x, int y, int w, int h);
	public abstract void drawOval(int x, int y, int w, int h);
	public abstract void drawPolyLine( int[] x, int[] y, int count);
	public abstract void drawLine(int x1, int y1, int x2, int y2);
	public abstract void draw(Shape shape);

	public abstract void fillRect(int x, int y, int w, int h);	
	public abstract void fillOval( int x, int y, int w, int h);


	public abstract void drawImage(BufferedImage bi, int x, int y);

}
