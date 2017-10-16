package spirite.base.image_data.images;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.ImageHandle;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
import spirite.hybrid.MDebug;

public class PrismaticInternalImage implements IInternalImage {
	private class LImg {
		int ox;
		int oy;
		int color;
		RawImage img;
		
		LImg() {}
		LImg(LImg other) {
			ox = other.ox;
			oy = other.oy;
			img = other.img.deepCopy();
		}
	}
	List<LImg> layers = new ArrayList<>();
	RawImage compositionImg;
	boolean compIsBuilt = false;
	Rect compRect = null;

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
			compositionImg = HybridHelper.createImage(1, 1);
		}
		else {
			compositionImg = HybridHelper.createImage(r.x, r.y);
			GraphicsContext gc = compositionImg.getGraphics();
			for( LImg img : layers) {
				gc.drawImage(img.img, img.ox, img.oy);
			}
		}
		
		compIsBuilt = true;
	}

	@Override
	public IBuiltImageData build(ImageHandle handle, int ox, int oy) {
		return new PrismaticBuiltImageData(handle, ox, oy);
	}

	@Override
	public IInternalImage dupe() {
		PrismaticInternalImage pii = new PrismaticInternalImage();
		
		pii.layers = new ArrayList<>(this.layers.size());
		for( LImg img : this.layers) {
			pii.layers.add(new LImg(img));
		}
		return pii;
	}

	boolean flushed = false;
	@Override
	public void flush() {
		if( !flushed) {
			for( LImg img : layers)
				img.img.flush();
			compositionImg.flush();
			flushed = true;
		}
	}
	@Override
	protected void finalize() throws Throwable {
		flush();
	}

	@Override
	public RawImage readOnlyAccess() {
		buildComposition();
		return compositionImg;
	}

	@Override
	public InternalImageTypes getType() {
		return InternalImageTypes.PRISMATIC;
	}

	public class PrismaticBuiltImageData extends IBuiltImageData {
		final int box;
		final int boy;
		RawImage buffer = null;
		int workingColor;
		
		public PrismaticBuiltImageData(ImageHandle handle, int ox, int oy) 
		{
			super(handle);
			this.box = ox;
			this.boy = oy;
			int workingColor = (handle.getContext().getPaletteManageR().getActiveColor(0))&0xFFFFFF;
		}

		@Override public int getWidth() {return handle.getContext().getWidth();}
		@Override public int getHeight() { return handle.getContext().getHeight();}

		@Override
		public void draw(GraphicsContext gc) {
			buildComposition();
			MatTrans transform = MatTrans.TranslationMatrix(box, boy);
			handle.drawLayer( gc, transform);
		}

		@Override
		public void drawBorder(GraphicsContext gc) {
			if( handle == null) return;
			
			gc.drawRect(box + compRect.x, boy + compRect.y, compRect.width, compRect.height);
		}

		@Override
		public GraphicsContext checkout() {
			return buffer.getGraphics();
		}

		@Override
		public RawImage checkoutRaw() {
			handle.getContext().getUndoEngine().prepareContext(handle);
			buffer = HybridHelper.createImage(handle.getContext().getWidth(), handle.getContext().getHeight());
			GraphicsContext gc = buffer.getGraphics();
			buildComposition();
			
			gc.drawHandle(this.handle, box + compRect.x, boy + compRect.y);
			
			//gc.drawHandle(this.handle, box+handle.getDynamicX(), boy+handle.getDynamicY());
			return buffer;
		}

		@Override
		public void checkin() {
			int w = handle.getContext().getWidth();
			int h = handle.getContext().getHeight();
			
			LImg img = null;
			for( LImg other : layers) {
				if( other.color == workingColor)
					img = other;
			}
			
			if( img == null) {
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
				gc.drawImage(buffer, -r.x, -r.y);
				
				img = new LImg();
				img.color = workingColor;
				img.img = nri;
				img.ox = r.x;
				img.oy = r.y;
				layers.add(img);
				compIsBuilt = false;
			}
//			Rect activeRect = (new Rect(0,0,w,h)).union(
//					new Rect(box+ox, boy+oy, handle.getWidth(), handle.getHeight()));
//
//			RawImage img = HybridHelper.createImage(w, h);
//			GraphicsContext gc = img.getGraphics();
//
//			// Draw the part of the old image over the new one
//			gc.drawHandle(handle,
//					box+ox - activeRect.x, 
//					boy+oy- activeRect.y);
//
//			// Clear the section of the old image that will be replaced by the new one
//			gc.setComposite( Composite.SRC, 1.0f);
//			gc.drawImage(buffer, -activeRect.x, -activeRect.y);
////				g2.dispose();
//
//			ox = activeRect.x - box;
//			oy = activeRect.y - boy;
//			activeRect = null;
//			
//			Rect cropped = null;
//			try {
//				cropped = HybridUtil.findContentBounds(img, 0, true);
//			} catch (UnsupportedImageTypeException e) {
//				e.printStackTrace();
//			}
//			
//			RawImage nri;
//			if( cropped == null || cropped.isEmpty()) {
//				nri = HybridHelper.createImage(1, 1);
//			}
//			else {
//				nri = HybridHelper.createImage( cropped.width, cropped.height);
//			}
//			gc = nri.getGraphics();
//			gc.drawImage( img, -cropped.x, -cropped.y);
//			
//			ox += cropped.x;
//			oy += cropped.y;
//			
//			cachedImage.relinquish(DynamicInternalImage.this);
//			cachedImage = handle.getContext().getCacheManager().cacheImage(nri, DynamicInternalImage.this);
//			
//			img.flush();
//			buffer = null;
//			gc = null;
			buffer.flush();
			buffer = null;

			// Construct ImageChangeEvent and send it
			handle.refresh();
		}

		@Override
		public MatTrans getScreenToImageTransform() {
			MatTrans transform = new MatTrans();
			transform.preTranslate( -box, -boy);
			return transform;
		}

		@Override public float convertX( float x) {return x;}
		@Override public float convertY( float y) {return y;}
		@Override public Vec2i convert(Vec2i p) {return p;}
		@Override
		public Rect getBounds() {
			return new Rect(box + compRect.x, boy + compRect.y, compRect.width, compRect.height);
		}

		@Override
		public MatTrans getCompositeTransform() {
			MatTrans trans = new MatTrans();
			trans.preTranslate(box, boy);
			return trans;
		}
	}
}
