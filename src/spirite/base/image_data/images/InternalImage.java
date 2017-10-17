package spirite.base.image_data.images;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.CacheManager.CachedImage;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;

/***
 * Normal Internal Image.  Has a RawImage (cached) that represents its image data
 * and that RawImage is drawn to.
 */
public class InternalImage implements IInternalImage {
	CachedImage cachedImage;
	protected final ImageWorkspace context;
	boolean flushed = false;
	
	public InternalImage( RawImage raw, ImageWorkspace context) { 
		this.context = context;
		this.cachedImage = context.getCacheManager().cacheImage(raw, this);
	}
	
	public int getWidth() {
		return cachedImage.access().getWidth();
	}
	public int getHeight() {
		return cachedImage.access().getHeight();
	}
	public IBuiltImageData build( BuildingImageData building) {
		return new BuiltImageData( building);
	}
	
	public InternalImage dupe() {
		return new InternalImage(cachedImage.access().deepCopy(), context);
	}
	public void flush() {
		if( !flushed) {
			cachedImage.relinquish(this);
			flushed = true;
		}
	}
	@Override
	protected void finalize() throws Throwable {
		flush();
	}
	@Override
	public RawImage readOnlyAccess() {
		return cachedImage.access();
	}
	
	public class BuiltImageData extends IBuiltImageData {
		final int ox;
		final int oy;
		
		public BuiltImageData( BuildingImageData building) {
			super(building.handle);
			this.ox = building.ox;
			this.oy = building.oy;
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
			
			context.getUndoEngine().prepareContext(handle);
			
			return cachedImage.access();
		}
		
		public void checkin() {
			//locked = false;
//			if( !handle.context.isValidHandle(handle))
//				return;

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

	@Override public int getDynamicX() {return 0;}
	@Override public int getDynamicY() {return 0;}
	@Override public InternalImageTypes getType() {return InternalImageTypes.NORMAL;}
}