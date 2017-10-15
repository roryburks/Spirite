package spirite.base.image_data;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.renderer.CacheManager.CachedImage;
import spirite.base.image_data.images.IBuiltImageData;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;

class DynamicInternalImage extends InternalImage {
		int ox, oy;
		DynamicInternalImage(CachedImage ci, int ox, int oy) {
			super(ci);
			this.ox = ox;
			this.oy = oy;
		}
		IBuiltImageData build( ImageHandle handle, int ox, int oy) {
			return new DynamicBuiltImageData(handle, ox, oy, this);
		}
		public class DynamicBuiltImageData extends IBuiltImageData{
			final int ox;
			final int oy;
			private final DynamicInternalImage dii;
			RawImage buffer = null;
			public DynamicBuiltImageData(ImageHandle handle, int ox, int oy,
					DynamicInternalImage dii) 
			{
				super(handle);
				this.ox = ox;
				this.oy = oy;
				this.dii = dii;
			}
			
			@Override public int getWidth() {
				return handle.context.width;
			} 
			@Override public int getHeight() {
				return handle.context.height;
			}
			@Override public float convertX( float x) {return x;}
			@Override public float convertY( float y) {return y;}
			@Override public Vec2i convert(Vec2i p) {return p;}
			@Override
			public void drawBorder(GraphicsContext gc) {
				if( handle == null) return;
				
				gc.drawRect(ox + dii.ox, oy + dii.oy, 
						handle.getWidth(), handle.getHeight());
			}
			@Override
			public MatTrans getCompositeTransform() {
				MatTrans trans = new MatTrans();
				trans.preTranslate(ox, oy);
				return trans;
			}
			
			@Override
			public GraphicsContext checkout() {
				return checkoutRaw().getGraphics();
			}
			
			@Override
			public Rect getBounds() {
				return new Rect( ox + dii.ox, oy + dii.oy, 
						handle.getWidth(), handle.getHeight());
			}
			@Override
			public RawImage checkoutRaw() {
				handle.context.undoEngine.prepareContext(handle);
				buffer = HybridHelper.createImage(handle.context.width, handle.context.height);
				GraphicsContext gc = buffer.getGraphics();
				gc.drawHandle(this.handle, ox+dii.ox, oy+dii.oy);
				return buffer;
			}
			@Override
			public void checkin() {
				int w = handle.context.width;
				int h = handle.context.height;
				Rect activeRect = (new Rect(0,0,w,h)).union(
						new Rect(ox+dii.ox, oy+dii.oy, handle.getWidth(), handle.getHeight()));

				RawImage img = HybridHelper.createImage(w, h);
				GraphicsContext gc = img.getGraphics();

				// Draw the part of the old image over the new one
				gc.drawHandle(handle,
						ox+dii.ox - activeRect.x, 
						oy+dii.oy- activeRect.y);

				// Clear the section of the old image that will be replaced by the new one
				gc.setComposite( Composite.SRC, 1.0f);
				gc.drawImage(buffer, -activeRect.x, -activeRect.y);
//				g2.dispose();

				dii.ox = activeRect.x - ox;
				dii.oy = activeRect.y - oy;
				activeRect = null;
				
				Rect cropped = null;
				try {
					cropped = HybridUtil.findContentBounds(img, 0, true);
				} catch (UnsupportedImageTypeException e) {
					e.printStackTrace();
				}
				
				RawImage nri;
				if( cropped == null || cropped.isEmpty()) {
					nri = HybridHelper.createImage(1, 1);
				}
				else {
					nri = HybridHelper.createImage( cropped.width, cropped.height);
				}
				gc = nri.getGraphics();
				gc.drawImage( img, -cropped.x, -cropped.y);
				
				dii.ox += cropped.x;
				dii.oy += cropped.y;
				
				handle.context.replaceCache( handle, nri);
				
				buffer.flush();
				img.flush();
				buffer = null;
				gc = null;

				// Construct ImageChangeEvent and send it
				handle.refresh();
			}

			@Override
			public void draw(GraphicsContext gc) {
				MatTrans transform = MatTrans.TranslationMatrix(ox, oy);
				handle.drawLayer( gc, transform);
			}

			@Override
			public MatTrans getScreenToImageTransform() {
				MatTrans transform = new MatTrans();
				transform.preTranslate( -ox, -oy);
				return transform;
			}
		}
	}