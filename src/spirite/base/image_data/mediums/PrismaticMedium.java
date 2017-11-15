package spirite.base.image_data.mediums;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.MatTrans.NoninvertableException;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

/***
 * PrismaticInternalImages are a type of Internal Image that behave similarly to 
 * Dynamic images, but separate each layer by color.
 */
public class PrismaticMedium implements IMedium {
	public static class LImg {
		// NOTE: Setting all of these to public SHOULD be fine so that
		//	PrismaticInternalImages can easily be externally built and loaded in
		//	but then you have to make sure you never pass the internal LImgs
		public int ox;
		public int oy;
		public int color;
		public RawImage img;
		
		public LImg() {}
		public LImg(LImg other, boolean deepcopy) {
			ox = other.ox;
			oy = other.oy;
			color = other.color;
			img = (deepcopy)?other.img.deepCopy():other.img;
		}
		public LImg(LImg other, boolean deepcopy, boolean copyForSave_IGNORED) {
			ox = other.ox;
			oy = other.oy;
			color = other.color;
			img = HybridUtil.copyForSaving(other.img);
		}
	}
	private List<LImg> layers = new ArrayList<>();
	private RawImage compositionImg;
	private boolean compIsBuilt = false;
	private Rect compRect = null;
	
	public PrismaticMedium() {}
	public PrismaticMedium( List<LImg> toImport) {
		for( LImg limg : toImport) {
			layers.add(new LImg(limg, false));
		}
	}

	@Override
	public int getWidth() {
		buildComposition();
		return compRect.width;
	}

	@Override
	public int getHeight() {
		buildComposition();
		return compRect.height;
	}

	@Override
	public int getDynamicX() {
		buildComposition();
		return compRect.x;
	}

	@Override
	public int getDynamicY() {
		buildComposition();
		return compRect.y;
	}
	private void buildComposition() {
		if( compIsBuilt)
			return;
		
		Rect r = new Rect( 0, 0, 0, 0);
		for( LImg img : layers)
			r = r.union(img.ox, img.oy, img.img.getWidth(), img.img.getHeight());
		compRect = r;
		
		if( compositionImg != null)
			compositionImg.flush();
		if( compRect == null || compRect.isEmpty()) {
			compositionImg = HybridHelper.createNillImage();
		}
		else {
			compositionImg = HybridHelper.createImage(r.width, r.height);
			GraphicsContext gc = compositionImg.getGraphics();
			for( LImg img : layers) {
				gc.drawImage(img.img, img.ox - r.x, img.oy - r.y);
			}
		}
		
		compIsBuilt = true;
	}

	public void drawBehind( GraphicsContext gc, int color) {
		for( LImg img : layers) {
			gc.drawImage(img.img, img.ox - compRect.x, img.oy - compRect.y);
			if( (img.color & 0xFFFFFF) == (color & 0xFFFFFF)) return;
		}
	}
	public void drawFront( GraphicsContext gc, int color) {
		boolean drawing = false;
		for( LImg img : layers) {
			if( drawing) 
				gc.drawImage(img.img, img.ox - compRect.x, img.oy - compRect.y);
			else if( (img.color & 0xFFFFFF) == (color & 0xFFFFFF)) 
				drawing = true;
		}
		
	}

	@Override
	public ABuiltMediumData build( BuildingMediumData building) {
		return new PrismaticBuiltImageData( building);
	}

	@Override
	public IMedium dupe() {
		PrismaticMedium pii = new PrismaticMedium();
		
		pii.layers = new ArrayList<>(this.layers.size());
		for( LImg img : this.layers) {
			pii.layers.add(new LImg(img, true));
		}
		return pii;
	}
	@Override
	public IMedium copyForSaving() {
		PrismaticMedium pii = new PrismaticMedium();
		
		pii.layers = new ArrayList<>(this.layers.size());
		for( LImg img : this.layers) {
			pii.layers.add(new LImg(img, true, true));
		}
		return pii;
	}
	
	public void moveLayer(int draggingFromIndex, int draggingToIndex) {
		layers.add(draggingToIndex, layers.remove(draggingFromIndex));
		compIsBuilt = false;
	}
	
	/** Note: the LImg's it returns are copies.  Changing the internal data will
	 * not change the PrismaticImage's data, but drawing to the image WILL effect
	 * the PrismaticImage. */
	public List<LImg> getColorLayers() {
		ArrayList<LImg> list = new ArrayList<LImg>(layers.size());
		
		for( LImg limg : layers) {
			list.add( new LImg(limg, false));
		}
		
		return list;
	}

	boolean flushed = false;
	@Override
	public void flush() {
		if( !flushed) {
			for( LImg img : layers)
				img.img.flush();
			if( compositionImg != null)
				compositionImg.flush();
			flushed = true;
		}
	}
	@Override
	protected void finalize() throws Throwable {
		flush();
	}

	@Override
	public IImage readOnlyAccess() {
		buildComposition();
		return compositionImg;
	}

	@Override
	public InternalImageTypes getType() {
		return InternalImageTypes.PRISMATIC;
	}

	public class PrismaticBuiltImageData extends ABuiltMediumData {
		final MatTrans trans;
		MatTrans invTrans;
		RawImage buffer = null;
		int workingColor;

		public PrismaticBuiltImageData(BuildingMediumData building) {
			super(building.handle);
			this.trans = building.trans;
			workingColor = building.color;
			try {
				this.invTrans = trans.createInverse();
			} catch (NoninvertableException e) {
				this.invTrans = new MatTrans();
			}
		}


		@Override public int getWidth() {return handle.getContext().getWidth();}
		@Override public int getHeight() { return handle.getContext().getHeight();}
		@Override public Vec2i convert(Vec2i p) {return p;}
		@Override public Vec2 convert(Vec2 p) {return p;}
		@Override public MatTrans getCompositeTransform() {return new MatTrans(trans);}
		@Override public MatTrans getScreenToImageTransform() {return new MatTrans(invTrans);}

		@Override public Rect getBounds() {return MUtil.circumscribeTrans( compRect, trans); }
		@Override
		public void drawBorder(GraphicsContext gc) {
			if( handle == null) return;
			
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawRect(compRect.x-1, compRect.y-1, 
					compRect.width+2, compRect.height+2);
			gc.setTransform(oldTrans);
		}
		@Override public void draw(GraphicsContext gc) {handle.drawLayer( gc, trans);}


		public GraphicsContext checkout() {
			return checkoutRaw().getGraphics();
		}

		public RawImage checkoutRaw() {
			handle.getContext().getUndoEngine().prepareContext(handle);
			buffer = HybridHelper.createImage(handle.getContext().getWidth(), handle.getContext().getHeight());
			GraphicsContext gc = buffer.getGraphics();
			buildComposition();

			LImg img = null;
			for( LImg other : layers) {
				if( other.color == workingColor)
					img = other;
			}
			if( img != null) {
				gc.setTransform(trans);
				gc.drawImage(img.img, img.ox, img.oy);
			}
			
			return buffer;
		}

		public void checkin() {
			int w = handle.getContext().getWidth();
			int h = handle.getContext().getHeight();
			
			LImg img = null;
			for( LImg other : layers) {
				if( other.color == workingColor)
					img = other;
			}
			
			if( img == null) {
				// Create new Layer for the color
				Rect r = null;
				try {
					r = HybridUtil.findContentBounds(buffer, 0, true);
				} catch (UnsupportedImageTypeException e) {
					MDebug.handleError(MDebug.ErrorType.STRUCTURAL, e, e.getMessage());
				}
				
				if( r == null || r.isEmpty())
					return;
				
				RawImage nri = HybridHelper.createImage(r.width, r.height); 
				GraphicsContext gc = nri.getGraphics();
				gc.setTransform(invTrans);
				gc.drawImage(buffer, -r.x, -r.y);
				
				img = new LImg();
				img.color = workingColor;
				img.img = nri;
				img.ox = r.x;
				img.oy = r.y;
				layers.add(img);
			}
			else {
				// Update the layer for the color: mostly duplicate code from DynamicInternalImage
				Rect drawAreaInImageSpace = MUtil.circumscribeTrans(new Rect(0,0,w,h), invTrans).union(
						new Rect(img.ox,img.oy, img.img.getWidth(), img.img.getHeight()));

				RawImage raw = HybridHelper.createImage(drawAreaInImageSpace.width, drawAreaInImageSpace.height);
				GraphicsContext gc = raw.getGraphics();

				// Draw the old image
				gc.drawImage(img.img, img.ox - drawAreaInImageSpace.x, img.oy - drawAreaInImageSpace.y);

				// Clear the section of the old image that will be replaced by the new one
				gc.transform(trans);
				gc.setComposite(Composite.SRC, 1.0f);
				gc.drawImage(buffer, 0, 0);
				
				Rect cropped = null;
				try {
					cropped = HybridUtil.findContentBounds( buffer, 0, true);
				} catch (UnsupportedImageTypeException e) 
					{MDebug.handleError(ErrorType.STRUCTURAL_MAJOR, e, e.getMessage());}
				
				RawImage nri;
				if( cropped == null || cropped.isEmpty()) {
					img.img.flush();
					layers.remove(img);
				}
				else {
					nri = HybridHelper.createImage(cropped.width, cropped.height);
					gc = nri.getGraphics();
					gc.drawImage( raw, -cropped.x, -cropped.y);

					img.ox = cropped.x-drawAreaInImageSpace.x;
					img.oy = cropped.y-drawAreaInImageSpace.y;
					
					img.img.flush();
					img.img = nri;
				}
			}
			compIsBuilt = false;
			buffer.flush();
			buffer = null;
		}


		@Override
		protected void _doOnGC(DoerOnGC doer) {
			doer.Do(checkout());
			checkin();
		}


		@Override
		protected void _doOnRaw(DoerOnRaw doer) {
			doer.Do(checkoutRaw());
			checkin();
		}



	}

	@Override public IImageDrawer getImageDrawer(BuildingMediumData building) {return new DefaultImageDrawer(this, building);}
}
