package spirite.graphics;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.image_data.GroupTree.GroupNode;
import spirite.pen.StrokeEngine;

/**
 * GraphicsContext is an abstract class which wraps all graphical functionality 
 * which can be rendered by different native engines (e.g. either OpenGL or the
 * basic Java AWT methods) depending on settings/device capability.
 * 
 * @author Rory Burks
 *
 */
public abstract class GraphicsContext {
	public abstract NodeRenderer createNodeRenderer( GroupNode node, RenderEngine context);
	public abstract StrokeEngine getStrokeEngine();
	
	public static interface RenderRoutine {
		public void render(GraphicsContext context);
	}
	
	/** Renders the described algorithm to a BufferedImge. */
	public abstract BufferedImage renderToImage( RenderRoutine renderable, int width, int height);
	
	
	
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
	public abstract void drawBounds(BufferedImage mask, int c, AffineTransform trans);
	
	public abstract void changeColor( BufferedImage image, Color from, Color to, int mode);
	public abstract void invert(BufferedImage image);
}
