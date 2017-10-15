package spirite.base.graphics;


import spirite.base.pen.StrokeEngine;

/**
 * Similar to GraphicsContext, GraphicsDrawer encapsulates certain graphics 
 * functionality to be implemented by multiple different engines, but unlike
 * GraphicsContext, GraphicsDrawer is not tied to any particular context,
 * instead either returning a BufferedImage with the result or performing the
 * changes in-place
 * 
 * @author Rory Burks
 *
 */
public abstract class GraphicsDrawer {
	public abstract StrokeEngine getStrokeEngine();

	public static interface RenderRoutine {
		public void render(GraphicsContext context);
	}
	
	public abstract void changeColor( RawImage image, int cFrom, int cTo, int mode);
	public abstract void invert(RawImage image);
}
