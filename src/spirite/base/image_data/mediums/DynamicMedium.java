package spirite.base.image_data.mediums;

import spirite.base.graphics.DynamicImage;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.MatTrans.NoninvertableException;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;
import spirite.base.util.linear.Vec2i;
import spirite.hybrid.HybridUtil;

/***
 * A Dynamic Internal Image is a kind of image that automatically resizes itself
 * to its content bounds as it is drawn on top of.  This slightly increases the 
 * time it takes to commit an image change, but reduces memory overhead as well as
 * the number of pixels pushed to re-draw the Workspace
 * 
 */
public class DynamicMedium implements IMedium {
	DynamicImage image;
	protected final ImageWorkspace context;
	boolean flushed = false;
	public DynamicMedium(RawImage raw, int ox, int oy, ImageWorkspace context) {
		this.context = context;
		this.image = new DynamicImage(context, raw, ox, oy);
	}
	DynamicMedium(DynamicImage image, ImageWorkspace context) {
		this.context = context;
		this.image = image;
	}
	public ABuiltMediumData build( BuildingMediumData building) {
		return new DynamicBuiltImageData(building);
	}
	@Override
	public IMedium dupe() {
		return new DynamicMedium( image.deepCopy(), context);
	}
	@Override
	public IMedium copyForSaving() {
		return new DynamicMedium( HybridUtil.copyForSaving(image.getBase()), image.getXOffset(), image.getYOffset(), context);
	}
	public int getWidth() {return image.getBase().getWidth();}
	public int getHeight() {return image.getBase().getHeight();}
	public void flush() {
		if( !flushed) {
			image.flush();
			flushed = true;
		}
	}
	@Override protected void finalize() throws Throwable {flush();}
	@Override public IImage readOnlyAccess() { return image.getBase();}
	@Override public int getDynamicX() {return image.getXOffset();}
	@Override public int getDynamicY() {return image.getYOffset();}
	@Override public InternalImageTypes getType() {return InternalImageTypes.DYNAMIC;}
	@Override public IImageDrawer getImageDrawer(BuildingMediumData building) { return new DefaultImageDrawer(this, building);}
	
	public class DynamicBuiltImageData extends ABuiltMediumData{
		MatTrans trans;
		MatTrans invTrans;
		RawImage buffer = null;
		
		public DynamicBuiltImageData(BuildingMediumData building) 
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
		@Override public MatTrans getCompositeTransform() {return new MatTrans(trans);}
		@Override public MatTrans getScreenToImageTransform() {return new MatTrans(invTrans);}
		@Override
		public Rect getBounds() {
			return image.getDrawBounds(trans);
		}
		@Override
		public void drawBorder(GraphicsContext gc) {
			if( handle == null) return;
			
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawRect(image.getXOffset()-1, image.getYOffset()-1, 
					handle.getWidth()+2, handle.getHeight()+2);
			gc.setTransform(oldTrans);
		}
		@Override
		public void draw(GraphicsContext gc) {
			handle.drawLayer( gc, trans);
		}
		

		@Override
		protected void _doOnGC(DoerOnGC doer) {
			image.doOnGC(doer, trans);
		}

		@Override
		protected void _doOnRaw(DoerOnRaw doer) {
			image.doOnRaw(doer, trans);
		}
	}
}