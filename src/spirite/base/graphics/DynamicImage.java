package spirite.base.graphics;

import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.mediums.ABuiltMediumData.DoerOnGC;
import spirite.base.image_data.mediums.ABuiltMediumData.DoerOnRaw;
import spirite.base.util.MUtil;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.MatTrans.NoninvertableException;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;

/**
 * DynamicImage
 * 	A wrapper for a RawImage which automatically resizes itself to 
 * 
 *	Although much of this is simply splitting DynamicMedium's code over two objects, since 
 *	other mediums commonly use this behavior, putting it in its own scope.
 */
public class DynamicImage {
	private ImageWorkspace context;
	private int ox, oy;
	private RawImage base;
	private RawImage buffer = null;
	private MatTrans trans = null;
	int checkoutCtr = 0;
	
	public DynamicImage( ImageWorkspace context, RawImage raw, int ox, int oy) {
		this.context = context;
		this.base = raw;
		this.ox = ox;
		this.oy = oy;
	}
	
	public int getXOffset() {return ox;}
	public int getYOffset() {return oy;}
	public int getWidth() {return (base == null) ? 0 : base.getWidth();}
	public int getHeight() {return (base == null) ? 0 : base.getHeight();}
	
	public RawImage getBase() {return base;}
	public ImageWorkspace getContext() {return context;}
	public void setContext( ImageWorkspace ws) {context = ws;}
	
	public void doOnGC( DoerOnGC doer, MatTrans transform) {
		doer.Do(checkoutRaw( transform).getGraphics());
		checkin();
	}
	public void doOnRaw( DoerOnRaw doer, MatTrans transform) {
		doer.Do(checkoutRaw( transform));
		checkin();
	}
	public Rect getDrawBounds( MatTrans transform) {
		return (new Rect( 0, 0, context.getWidth(), context.getHeight())).union(
				MUtil.circumscribeTrans(new Rect(ox, oy, base.getWidth(), base.getHeight()), transform));
	}

	int checkoutCnt = 0;
	private RawImage checkoutRaw(MatTrans transform) {
		if( checkoutCnt++ == 0) {
			this.trans = transform;
			buffer = HybridHelper.createImage(context.getWidth(), context.getHeight());
			GraphicsContext gc = buffer.getGraphics();
			gc.setTransform(trans);
			gc.drawImage(base, ox, oy);
		}
		return buffer;
	}
	
	
	
	private void checkin() {
		if( --checkoutCnt == 0) {
			int w = context.getWidth();
			int h = context.getHeight();
			
			MatTrans invTrans;
	
			try {
				invTrans = trans.createInverse();
			} catch (NoninvertableException e) {
				invTrans = new MatTrans();
			}
			
			Rect drawAreaInImageSpace = MUtil.circumscribeTrans(new Rect(0,0,w,h), invTrans).union(
							new Rect(ox,oy, base.getWidth(), base.getHeight()));
	
			RawImage img = HybridHelper.createImage(drawAreaInImageSpace.width, drawAreaInImageSpace.height);
			GraphicsContext gc = img.getGraphics();
	
			// Draw the old image
			gc.drawImage(base, ox - drawAreaInImageSpace.x, oy - drawAreaInImageSpace.y);
	
			// Clear the section of the old image that will be replaced by the new one
			gc.transform(trans);
			gc.setComposite( Composite.SRC, 1.0f);
			gc.drawImage(buffer, 0, 0);
	//			g2.dispose();
	
			
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
			
			base.flush();
			base = nri;
			
			buffer.flush();
			img.flush();
			buffer = null;
			gc = null;
		}
	}
	
	public void flush() {
		if( base != null) {
			base.flush();
			base = null;
		}
	}
	
	public DynamicImage deepCopy() {
		DynamicImage copy = new DynamicImage(getContext(), base.deepCopy(), ox, oy);
		return copy;
	}
}
