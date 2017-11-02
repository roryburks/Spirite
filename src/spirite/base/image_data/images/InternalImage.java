package spirite.base.image_data.images;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.CacheManager.CachedImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.images.drawer.DefaultImageDrawer;
import spirite.base.image_data.images.drawer.IImageDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.glmath.MatTrans.NoninvertableException;
import spirite.hybrid.HybridUtil;

/***
 * Normal Internal Image.  Has a RawImage (cached) that represents its image data
 * and that RawImage is drawn to.
 */
public class InternalImage implements IInternalImage {
	RawImage image;
	protected final ImageWorkspace context;
	boolean flushed = false;
	
	public InternalImage( RawImage raw, ImageWorkspace context) { 
		this.context = context;
		this.image = raw;
	}
	
	public int getWidth() {
		return image.getWidth();
	}
	public int getHeight() {
		return image.getHeight();
	}
	public ABuiltImageData build( BuildingImageData building) {
		return new BuiltImageData( building);
	}
	
	@Override public InternalImage dupe() {return new InternalImage(image.deepCopy(), context);}
	@Override public IInternalImage copyForSaving() {return new InternalImage( HybridUtil.copyForSaving(image), context);}
	public void flush() {
		if( !flushed) {
			image.flush();
			flushed = true;
		}
	}
	@Override
	protected void finalize() throws Throwable {
		flush();
	}
	@Override
	public RawImage readOnlyAccess() {
		return image;
	}
	
	public class BuiltImageData extends ABuiltImageData {
		MatTrans trans;
		MatTrans invTrans;
		
		public BuiltImageData( BuildingImageData building) {
			super(building.handle);
			this.trans = building.trans;
			try {
				this.invTrans = trans.createInverse();
			} catch (NoninvertableException e) {
				this.invTrans = new MatTrans();
			}
		}
		
		public int getWidth() {
			return handle.getWidth();
		}
		public int getHeight() {
			return handle.getHeight();
		}
		
		public void draw(GraphicsContext gc) {
			handle.drawLayer( gc, trans);
		}
		
		public void drawBorder( GraphicsContext gc) {
			if( handle == null) return;
			
			MatTrans transform = gc.getTransform();
			gc.preTransform(trans);
			
			gc.drawRect(0, 0, handle.getWidth(), handle.getHeight());
			
			gc.setTransform( transform);
		}
		
		public GraphicsContext checkout() {
			GraphicsContext gc = _checkoutImage().getGraphics();
			gc.preTransform(invTrans);
			return gc;
		}
		
		public RawImage checkoutRaw() {
			return _checkoutImage();
		}
		
		private RawImage _checkoutImage() {
			ImageWorkspace context = handle.getContext();
			
			context.getUndoEngine().prepareContext(handle);
			
			return image;
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
			Vec2 inverted = invTrans.transform(new  Vec2(p.x, p.y), new Vec2());
			return new Vec2i((int)inverted.x, (int)inverted.y);
		}
		public Vec2 convert( Vec2 p) {
			//	Some image modification methods do not use draw actions, but
			//	 rather alter the image directly.  For example a flood fill action.
			//	
			return invTrans.transform(new  Vec2(p.x, p.y), new Vec2());
			
		}
		
		public Rect getBounds() {
			return MUtil.circumscribeTrans(new Rect(0,0,getWidth(), getHeight()), trans);
		}
		
		public MatTrans getScreenToImageTransform() {
			return new MatTrans(invTrans);
		}

		public MatTrans getCompositeTransform() {
			return new MatTrans();			
		}
	}

	@Override public int getDynamicX() {return 0;}
	@Override public int getDynamicY() {return 0;}
	@Override public InternalImageTypes getType() {return InternalImageTypes.NORMAL;}
	@Override public IImageDrawer getImageDrawer(BuildingImageData building) {return new DefaultImageDrawer(this, building);}

}