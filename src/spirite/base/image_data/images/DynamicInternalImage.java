package spirite.base.image_data.images;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.CacheManager.CachedImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.images.drawer.DefaultImageDrawer;
import spirite.base.image_data.images.drawer.IImageDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.MatTrans.NoninvertableException;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
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
	public ABuiltImageData build( BuildingImageData building) {
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
	@Override public IImageDrawer getImageDrawer(BuildingImageData building) { return new DefaultImageDrawer(this, building);}
	
	public class DynamicBuiltImageData extends ABuiltImageData{
		MatTrans trans;
		MatTrans invTrans;
		RawImage buffer = null;
		
		public DynamicBuiltImageData(BuildingImageData building) 
		{
			super(building.handle);
			this.trans = building.trans;
			try {
				this.invTrans = trans.createInverse();
			} catch (NoninvertableException e) {
				this.invTrans = new MatTrans();
			}
		}
		
		@Override public int getWidth() {return handle.getContext().getWidth();} 
		@Override public int getHeight() {return handle.getContext().getHeight();}
		@Override public Vec2i convert(Vec2i p) {return p;}
		@Override public Vec2 convert(Vec2 p) {return p;}
		@Override
		public void drawBorder(GraphicsContext gc) {
			if( handle == null) return;
			
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawRect(ox-1, oy-1, 
					handle.getWidth()+2, handle.getHeight()+2);
			gc.setTransform(oldTrans);
		}
		@Override public MatTrans getCompositeTransform() 
			{return new MatTrans(trans);}
		@Override public MatTrans getScreenToImageTransform() 
			{return new MatTrans(invTrans);}
		@Override
		public Rect getBounds() {
			return MUtil.circumscribeTrans( new Rect(ox, oy, handle.getWidth(), handle.getHeight()), trans);
		}
		
		@Override
		public GraphicsContext checkout() {
			return checkoutRaw().getGraphics();
		}
		
		@Override
		public RawImage checkoutRaw() {
			handle.getContext().getUndoEngine().prepareContext(handle);
			buffer = HybridHelper.createImage(handle.getContext().getWidth(), handle.getContext().getHeight());
			GraphicsContext gc = buffer.getGraphics();
			gc.setTransform(trans);
			gc.drawHandle(this.handle, handle.getDynamicX(), handle.getDynamicY());
			return buffer;
		}
		@Override
		public void checkin() {
			int w = handle.getContext().getWidth();
			int h = handle.getContext().getHeight();
			
			
			Rect drawAreaInImageSpace = MUtil.circumscribeTrans(new Rect(0,0,w,h), invTrans).union(
							new Rect(ox,oy, getWidth(), getHeight()));

			RawImage img = HybridHelper.createImage(w, h);
			GraphicsContext gc = img.getGraphics();

			// Draw the old image
			gc.drawHandle(handle, ox - drawAreaInImageSpace.x, oy - drawAreaInImageSpace.y);

			// Clear the section of the old image that will be replaced by the new one
			gc.transform(trans);
			gc.setComposite( Composite.SRC, 1.0f);
			gc.drawImage(buffer, 0, 0);
//				g2.dispose();

			
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
			
			ox = cropped.x-drawAreaInImageSpace.x;
			oy = cropped.y-drawAreaInImageSpace.y;
			
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
			handle.drawLayer( gc, trans);
		}

	}


}