package spirite.base.image_data.images;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.CacheManager.CachedImage;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;

/***
 * A Dynamic Internal Image is a kind of image that automatically resizes itself
 * to its content bounds as it is drawn on top of.  This slightly increases the 
 * time it takes to commit an image change, but reduces memory overhead as well as
 * the number of pixels pushed to re-draw the Workspace
 * 
 */
public class DynamicInternalImage implements IInternalImage {
	CachedImage cachedImage;
	protected final ImageWorkspace context;
	boolean flushed = false;
	
	int ox, oy;
	public DynamicInternalImage(RawImage raw, int ox, int oy, ImageWorkspace context) {
		this.context = context;
		this.cachedImage = context.getCacheManager().cacheImage(raw, this);
		this.ox = ox;
		this.oy = oy;
	}
	public IBuiltImageData build( BuildingImageData building) {
		return new DynamicBuiltImageData(building);
	}
	@Override
	public IInternalImage dupe() {
		return new DynamicInternalImage( cachedImage.access().deepCopy(), ox, oy, context);
	}
	public int getWidth() {return cachedImage.access().getWidth();}
	public int getHeight() {return cachedImage.access().getHeight();}
	public void flush() {
		if( !flushed) {
			cachedImage.relinquish(this);
			flushed = true;
		}
	}
	@Override protected void finalize() throws Throwable {flush();}
	@Override public RawImage readOnlyAccess() { return cachedImage.access();}
	@Override public int getDynamicX() {return ox;}
	@Override public int getDynamicY() {return oy;}
	@Override public InternalImageTypes getType() {return InternalImageTypes.DYNAMIC;}
	
	public class DynamicBuiltImageData extends IBuiltImageData{
		final int box;
		final int boy;
		RawImage buffer = null;
		
		public DynamicBuiltImageData(BuildingImageData building) 
		{
			super(building.handle);
			this.box = building.ox;
			this.boy = building.oy;
		}
		
		@Override public int getWidth() {
			return handle.getContext().getWidth();
		} 
		@Override public int getHeight() {
			return handle.getContext().getHeight();
		}
		@Override public float convertX( float x) {return x;}
		@Override public float convertY( float y) {return y;}
		@Override public Vec2i convert(Vec2i p) {return p;}
		@Override
		public void drawBorder(GraphicsContext gc) {
			if( handle == null) return;
			
			gc.drawRect(box + ox, boy + oy, 
					handle.getWidth(), handle.getHeight());
		}
		@Override
		public MatTrans getCompositeTransform() {
			MatTrans trans = new MatTrans();
			trans.preTranslate(box, boy);
			return trans;
		}
		
		@Override
		public GraphicsContext checkout() {
			return checkoutRaw().getGraphics();
		}
		
		@Override
		public Rect getBounds() {
			return new Rect( box + handle.getDynamicX(), boy + handle.getDynamicY(), 
					handle.getWidth(), handle.getHeight());
		}
		@Override
		public RawImage checkoutRaw() {
			handle.getContext().getUndoEngine().prepareContext(handle);
			buffer = HybridHelper.createImage(handle.getContext().getWidth(), handle.getContext().getHeight());
			GraphicsContext gc = buffer.getGraphics();
			gc.drawHandle(this.handle, box+handle.getDynamicX(), boy+handle.getDynamicY());
			return buffer;
		}
		@Override
		public void checkin() {
			int w = handle.getContext().getWidth();
			int h = handle.getContext().getHeight();
			
			Rect activeRect = (new Rect(0,0,w,h)).union(
					new Rect(box+ox, boy+oy, handle.getWidth(), handle.getHeight()));

			RawImage img = HybridHelper.createImage(w, h);
			GraphicsContext gc = img.getGraphics();

			// Draw the part of the old image over the new one
			gc.drawHandle(handle,
					box+ox - activeRect.x, 
					boy+oy- activeRect.y);

			// Clear the section of the old image that will be replaced by the new one
			gc.setComposite( Composite.SRC, 1.0f);
			gc.drawImage(buffer, -activeRect.x, -activeRect.y);
//				g2.dispose();

			ox = activeRect.x - box;
			oy = activeRect.y - boy;
			activeRect = null;
			
			Rect cropped = null;
			try {
				cropped = HybridUtil.findContentBounds(img, 0, true);
			} catch (UnsupportedImageTypeException e) {
				e.printStackTrace();
			}
			
			RawImage nri;
			if( cropped == null || cropped.isEmpty()) {
				nri = HybridHelper.createNillImage();
			}
			else {
				nri = HybridHelper.createImage( cropped.width, cropped.height);
			}
			gc = nri.getGraphics();
			gc.drawImage( img, -cropped.x, -cropped.y);
			
			ox += cropped.x;
			oy += cropped.y;
			
			cachedImage.relinquish(DynamicInternalImage.this);
			cachedImage = handle.getContext().getCacheManager().cacheImage(nri, DynamicInternalImage.this);
			
			buffer.flush();
			img.flush();
			buffer = null;
			gc = null;

			// Construct ImageChangeEvent and send it
			handle.refresh();
		}

		@Override
		public void draw(GraphicsContext gc) {
			MatTrans transform = MatTrans.TranslationMatrix(box, boy);
			handle.drawLayer( gc, transform);
		}

		@Override
		public MatTrans getScreenToImageTransform() {
			MatTrans transform = new MatTrans();
			transform.preTranslate( -box, -boy);
			return transform;
		}
	}

}