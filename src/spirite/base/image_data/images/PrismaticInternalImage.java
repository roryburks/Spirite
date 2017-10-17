package spirite.base.image_data.images;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import spirite.hybrid.MDebug.ErrorType;

public class PrismaticInternalImage implements IInternalImage {
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
	}
	private List<LImg> layers = new ArrayList<>();
	private RawImage compositionImg;
	private boolean compIsBuilt = false;
	private Rect compRect = null;
	
	public PrismaticInternalImage() {}
	public PrismaticInternalImage( List<LImg> toImport) {
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
			compositionImg = HybridHelper.createImage(1, 1);
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

	@Override
	public IBuiltImageData build(ImageHandle handle, int ox, int oy) {
		return new PrismaticBuiltImageData(handle, ox, oy);
	}

	@Override
	public IInternalImage dupe() {
		PrismaticInternalImage pii = new PrismaticInternalImage();
		
		pii.layers = new ArrayList<>(this.layers.size());
		for( LImg img : this.layers) {
			pii.layers.add(new LImg(img, true));
		}
		return pii;
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
			workingColor = (handle.getContext().getPaletteManageR().getActiveColor(0))&0xFFFFFF;
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
			return checkoutRaw().getGraphics();
		}

		@Override
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
			if( img != null)
				gc.drawImage(img.img, img.ox + box, img.oy + boy);
			
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
				Rect activeRect = (new Rect(0,0,w,h))
						.union(img.ox+box, img.oy+boy, img.img.getWidth(), img.img.getHeight());
				
				RawImage raw = HybridHelper.createImage(activeRect.width, activeRect.height);
				GraphicsContext gc = raw.getGraphics();

				// Draw the part of the old image over the new one
				gc.drawImage(img.img, box+img.ox-activeRect.x, boy+img.oy-activeRect.y);

				// Clear the section of the old image that will be replaced by the new one
				gc.setComposite(Composite.SRC, 1.0f);
				gc.drawImage(buffer,  -activeRect.x, -activeRect.y);
				
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

					img.ox = activeRect.x - box + cropped.x;
					img.oy = activeRect.y - boy + cropped.y;
					
					img.img.flush();
					img.img = nri;
				}
			}
			compIsBuilt = false;
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
