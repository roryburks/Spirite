package spirite.base.graphics;

import java.awt.Shape;	// TODO

import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.RawImage;
import spirite.base.util.glmath.MatTrans;

/**
 * GraphicsContext is an abstract class which wraps all graphical functionality 
 * which can be rendered by different native engines (e.g. either OpenGL or the
 * basic Java AWT methods) depending on settings/device capability.
 * 
 * @author Rory Burks
 *
 */
public abstract class GraphicsContext {
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
	public abstract void drawBounds(RawImage bi, int c);

	public abstract void clear();
	
	/** Setting to null produces undefined behavior. */
	public abstract void setTransform( MatTrans trans);
	public abstract MatTrans getTransform();
	public abstract void translate(double offsetX, double offsetY);
	public abstract void transform(MatTrans trans);
	public abstract void scale(double sx, double sy);
	public abstract void setColor(int argb);
	
	public enum Composite {
		SRC, SRC_IN, SRC_OVER, SRC_OUT, SRC_ATOP,
		DST, DST_IN, DST_OVER, DST_OUT, DST_ATOP,
		CLEAR, XOR,
	};
	public abstract void setComposite( Composite composite, float alpha);
	public abstract float getAlpha();
	public abstract Composite getComposite();
	
	public enum JoinMethod {MITER, ROUNDED, BEVEL};
	public enum CapMethod {NONE, ROUND, SQUARE};
	public static class LineAttributes {
		public final float width;
		public final JoinMethod join;
		public final CapMethod cap;
		public final float[] dashes;
		
		public LineAttributes( float width, CapMethod cap, JoinMethod join) {
			this.width = width;
			this.join = join;
			this.cap = cap;
			this.dashes = null;
		}
		public LineAttributes( float width, CapMethod cap, JoinMethod join, float[] dashes) {
			this.width = width;
			this.join = join;
			this.cap = cap;
			this.dashes = dashes;
		}
	}
	public abstract void setLineAttributes( LineAttributes line);
	/** May return null if the underlying engine's Line Attributes aren't 
	 * representable by the generic LineAttributes class. */
	public abstract LineAttributes getLineAttributes();

	public abstract void drawRect(int x, int y, int w, int h);
	public abstract void drawOval(int x, int y, int w, int h);
	public abstract void drawPolyLine( int[] x, int[] y, int count);
	public abstract void drawLine(int x1, int y1, int x2, int y2);
	public abstract void draw(Shape shape);

	public abstract void fillRect(int x, int y, int w, int h);	
	public abstract void fillOval( int x, int y, int w, int h);


	public abstract void drawImage( RawImage img, int x, int y);
	public abstract void drawHandle( ImageHandle handle, int x, int y);

	public abstract void fillPolygon(int[] x, int[] y, int count);

	public abstract void setClip(int i, int j, int width, int height);

	/** Marks the Graphic Context as no longer being used.  Strictly speaking, 
	 * calling this method shouldn't be necessary, but that relies on native
	 * implementations of AWT's Graphics context.
	 * */
	public abstract void dispose();


	public abstract void renderImage(RawImage rawImage, int x, int y, RenderProperties render);
	public abstract void renderHandle( ImageHandle handle, int x, int y, RenderProperties render);



}
