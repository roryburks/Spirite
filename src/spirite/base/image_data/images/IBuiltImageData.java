package spirite.base.image_data.images;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageHandle;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;

// ===============
// ==== Data Building
public abstract class IBuiltImageData {
	public final ImageHandle handle;
	
	public IBuiltImageData( ImageHandle handle) {this.handle = handle;}
	
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract void draw(GraphicsContext gc);
	public abstract void drawBorder( GraphicsContext gc);

	/**
	 * Creates a graphical object with transforms applied such that
	 * drawing on the returned Graphics will draw on the correct Image
	 * Data spot.
	 * 
	 * !!! When done modifying the image always call checkout. !!!
	 */
	public abstract GraphicsContext checkout();

	/** Retrieves the underlying BufferedImage of the BuiltImage
	 * 
	 * !!! When done modifying the image always call checkout. !!!
	 */
	public abstract RawImage checkoutRaw();

	/**
	 * Once finished drawn you must checkin your data.  Not only does this
	 * dispose the Graphics (which is debatably necessary), but it triggers
	 * the appropriate ImageChange actions and re-fits the Dynamic data.
	 */
	public abstract void checkin();
	
	/** Returns a transform converting from screen space to layer space. */
	public abstract MatTrans getScreenToImageTransform();

	/** Converts the given point in ImageSpace to BuiltActiveData space*/
	public abstract float convertX( float x);
	public abstract float convertY( float y);
	public abstract Vec2i convert( Vec2i p);

	public abstract Rect getBounds();

	/** Returns a transform representing how to convert the image from its internal
	 * image space to a composited image space (for normal Images, this is the
	 * Identity Matrix, for DynamicImages, since they allow editing anywhere on the
	 * screen, this is equal to the conversion from layerspace to screen space)*/
	public abstract MatTrans getCompositeTransform();
}