package spirite.base.image_data;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.CacheManager.CachedImage;
import spirite.base.image_data.images.IBuiltImageData;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;

/**
 * Note: For Organization purposes I really wish I could insert this into the image_data.images package,
 * but it needed far too much internal scope access.
 */
class InternalImage {
	CachedImage cachedImage;
	InternalImage( CachedImage ci) { this.cachedImage = ci;}
	
	int getWidth() {
		return cachedImage.access().getWidth();
	}
	int getHeight() {
		return cachedImage.access().getHeight();
	}
	IBuiltImageData build( ImageHandle handle, int ox, int oy) {
		return new BuiltImageData(handle);
	}
	public class BuiltImageData extends IBuiltImageData {
		final int ox;
		final int oy;
		
		public BuiltImageData( ImageHandle handle) {
			super(handle);
			this.ox = 0;
			this.oy = 0;
		}
		public BuiltImageData( ImageHandle handle, int ox, int oy) {
			super(handle);
			this.ox = ox;
			this.oy = oy;
		}
		
		public int getWidth() {
			return handle.getWidth();
		}
		public int getHeight() {
			return handle.getHeight();
		}
		
		public void draw(GraphicsContext gc) {
			MatTrans transform = MatTrans.TranslationMatrix(ox, oy);
			handle.drawLayer( gc, transform);
		}
		
		public void drawBorder( GraphicsContext gc) {
			if( handle == null) return;
			
			MatTrans transform = gc.getTransform();
			gc.translate(ox, oy);
			
			gc.drawRect(0, 0, handle.getWidth(), handle.getHeight());
			
			gc.setTransform( transform);
		}
		
		public GraphicsContext checkout() {
			GraphicsContext gc = _checkoutImage().getGraphics();
			gc.translate(-ox, -oy);
			return gc;
		}
		
		public RawImage checkoutRaw() {
			return _checkoutImage();
		}
		
		private RawImage _checkoutImage() {
			ImageWorkspace context = handle.getContext();
			if( !context.isValidHandle(handle))
				return null;
			
			context.getUndoEngine().prepareContext(handle);
			
			return  context.imageData.get(handle.id).cachedImage.access();
		}
		
		public void checkin() {
			//locked = false;
			if( !handle.context.isValidHandle(handle))
				return;

			// Construct ImageChangeEvent and send it
			handle.refresh();
		}
		
		/** Converts the given point in ImageSpace to BuiltActiveData space*/
		public Vec2i convert( Vec2i p) {
			//	Some image modification methods do not use draw actions, but
			//	 rather alter the image directly.  For example a flood fill action.
			//	
			return new Vec2i(p.x-ox, p.y-oy);
		}
		public float convertX( float x) {return x - ox;}
		public float convertY( float y) {return y - oy;}
		
		public Rect getBounds() {
			return new Rect( ox, oy, handle.getWidth(), handle.getHeight());
		}
		
		public MatTrans getScreenToImageTransform() {
			MatTrans transform = new MatTrans();
			transform.preTranslate( -ox, -oy);
			return transform;
		}

		public MatTrans getCompositeTransform() {
			return new MatTrans();			
		}
	}
}