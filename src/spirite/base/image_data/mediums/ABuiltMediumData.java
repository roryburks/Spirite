package spirite.base.image_data.mediums;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.MediumHandle;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

// ===============
// ==== Data Building
/***
 * An IBuiltImageData is an interface for dealing with data that has a variety of transforms 
 * and scopes imposed on it.  The IBuiltImageData interface is meant to be short-lived,
 * representing Data that has been linked up to be written to and shouldn't be stored
 * over long terms.
 *
 */
public abstract class ABuiltMediumData {
	public final MediumHandle handle;
	
	public ABuiltMediumData( MediumHandle handle) {this.handle = handle;}
	
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract void draw(GraphicsContext gc);
	public abstract void drawBorder( GraphicsContext gc);

	public interface DoerOnGC {
		public void Do( GraphicsContext gc);
	}
	public interface DoerOnRaw {
		public void Do( RawImage raw);
	}

	private boolean doing = false;
	public final void doOnGC( DoerOnGC doer) {
		if( doing) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to recursively check-out");
			return;
		}
		handle.getContext().getUndoEngine().prepareContext(handle);
		doing = true;
		_doOnGC(doer);
		doing = false;
		
		handle.refresh();
	}
	public final void doOnRaw( DoerOnRaw doer) {
		if( doing) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to recursively check-out");
			return;
		}
		handle.getContext().getUndoEngine().prepareContext(handle);
		doing = true;
		_doOnRaw(doer);
		doing = false;
		
		handle.refresh();
	}
	protected abstract void _doOnGC( DoerOnGC doer);
	protected abstract void _doOnRaw( DoerOnRaw doer);
	
	
	/** Returns a transform converting from screen space to layer space. */
	public abstract MatTrans getScreenToImageTransform();

	/** Converts the given point in ImageSpace to BuiltActiveData space*/
	public abstract Vec2i convert( Vec2i p);
	public abstract Vec2 convert( Vec2 p);

	public abstract Rect getBounds();

	/** Returns a transform representing how to convert the image from its internal
	 * image space to a composited image space (for normal Images, this is the
	 * Identity Matrix, for DynamicImages, since they allow editing anywhere on the
	 * screen, this is equal to the conversion from layerspace to screen space)*/
	public abstract MatTrans getCompositeTransform();
}