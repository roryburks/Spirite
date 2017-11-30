package spirite.base.image_data.mediums;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.MatTrans.NoninvertableException;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;
import spirite.base.util.linear.Vec2i;
import spirite.hybrid.HybridUtil;

/***
 * Normal Internal Image.  Has a RawImage (cached) that represents its image data
 * and that RawImage is drawn to.
 */
public class FlatMedium implements IMedium {
	private final RawImage image;
	protected final ImageWorkspace context;
	boolean flushed = false;
	
	public FlatMedium( RawImage raw, ImageWorkspace context) {
		this.context = context;
		this.image = raw;
	}
	
	public int getWidth() {
		return image.getWidth();
	}
	public int getHeight() {
		return image.getHeight();
	}
	public ABuiltMediumData build( BuildingMediumData building) {
		return new BuiltImageData( building);
	}
	
	@Override public FlatMedium dupe() {return new FlatMedium(image.deepCopy(), context);}
	@Override public IMedium copyForSaving() {return new FlatMedium( HybridUtil.copyForSaving(image), context);}
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
	public IImage readOnlyAccess() {
		return image;
	}
	
	public class BuiltImageData extends ABuiltMediumData {
		MatTrans trans;
		MatTrans invTrans;
		
		public BuiltImageData( BuildingMediumData building) {
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
		
		/** Converts the given point in ImageSpace to BuiltActiveData space*/
		public Vec2i convert( Vec2i p) {
			//	Some image modification methods do not use draw actions, but
			//	 rather alter the image directly.  For example a flood fill action.
			//	
			Vec2 inverted = invTrans.transform(new  Vec2(p.x, p.y));
			return new Vec2i((int)inverted.x, (int)inverted.y);
		}
		public Vec2 convert( Vec2 p) {
			//	Some image modification methods do not use draw actions, but
			//	 rather alter the image directly.  For example a flood fill action.
			//	
			return invTrans.transform(new  Vec2(p.x, p.y));
			
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

		@Override
		protected void _doOnGC(DoerOnGC doer) {
			doer.Do( image.getGraphics());
		}

		@Override
		protected void _doOnRaw(DoerOnRaw doer) {
			doer.Do( image);
		}
	}

	@Override public int getDynamicX() {return 0;}
	@Override public int getDynamicY() {return 0;}
	@Override public InternalImageTypes getType() {return InternalImageTypes.NORMAL;}
	@Override public IImageDrawer getImageDrawer(BuildingMediumData building) {return new DefaultImageDrawer(this, building);}

}