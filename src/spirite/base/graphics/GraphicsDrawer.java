package spirite.base.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.pen.StrokeEngine;
import spirite.base.image_data.RawImage;

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
	public abstract NodeRenderer createNodeRenderer( GroupNode node, RenderEngine context);
	public abstract StrokeEngine getStrokeEngine();

	public static interface RenderRoutine {
		public void render(GraphicsContext context);
	}
	
	/** Renders the described algorithm to a BufferedImge. */
	public abstract BufferedImage renderToImage( RenderRoutine renderable, int width, int height);
	
	public abstract void changeColor( RawImage image, int cFrom, int cTo, int mode);
	public abstract void invert(RawImage image);
}
