package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.image_data.ImageWorkspace.DynamicInternalImage;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.InternalImage;

/**
 * Under normal circumstances an ImageHandle is a logical connection to a
 * CachedImage in an ImageWorkspace.  It also helps smooth out the process
 * of building complex data structures relying on Images that should be 
 * Cached by allowing you to create ImageHandles without a null context
 * and then linking them to the ImageWorkspace using importData
 * 
 * !!!! NOTE: Use .equals().  There should be little to no reason to ever
 * use <code>==</code> as it defeats the entire point of handles.
 */
public class ImageHandle {
	ImageWorkspace context;
	
	int id = -1;
	
	// ImageWorkspace.getData should have constant-time access (being implemented
	//	with a HashTable, so there's probably no point to remember the
	//	CachedImage and doing so might lead to unnecessary bugs.
	
	public ImageHandle( ImageWorkspace context, int id) {
		this.context = context;
		this.id = id;
	}
	
	/** Returns a null-context duplicate (just preserves the ID) */
	public ImageHandle dupe() {
		return new ImageHandle( null, this.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if( !( obj instanceof ImageHandle))
			return false;
		ImageHandle other = (ImageHandle)obj;
		
		return (this.context == other.context) && (this.id == other.id);
	}
	
	/** Might not be necessary or belong. */
	public ImageWorkspace getContext() {
		return context;
	}
	
	/** Should only be used for reading/copying in things that need direct
	 * access to the BufferedImage.
	 * 
	 * RETURN VALUE SHOULD NEVER BE STORED LONG-TERM, if used for writing, 
	 * will not trigger proper Observers.  And probably other bad stuff 
	 * will happen if it sticks around in GC
	 *  */
	public BufferedImage deepAccess() {
		if( context == null) return null;
		return context.getData(id).cachedImage.access();
	}

	public void drawLayer(Graphics g, AffineTransform transform, Composite composite) {
		Graphics2D g2 = (Graphics2D)g;
		Composite c = g2.getComposite();
		
		if( composite != null) {
			if( composite instanceof AlphaComposite && c instanceof AlphaComposite) {
	
				g2.setComposite( AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 
						((AlphaComposite)composite).getAlpha()*
						((AlphaComposite)c).getAlpha()));
			}
			else
				g2.setComposite(composite);
		}
		drawLayer(g,transform);
		
//		Globals.BI_FORMAT;
		g2.setComposite(c);
	}
	
	public boolean isDynamic() {
		if( context != null && context.getData(id) instanceof DynamicInternalImage)
			return true;
		return false;
	}
	public int getDynamicX() {
		if( context == null) return 0;
		InternalImage ii = context.getData(id);
		if( ii instanceof DynamicInternalImage) {
			return ((DynamicInternalImage) ii).ox;
		}
		return 0;
	}
	public int getDynamicY() {
		if( context == null) return 0;
		InternalImage ii = context.getData(id);
		if( ii instanceof DynamicInternalImage) {
			return ((DynamicInternalImage) ii).oy;
		}
		return 0;
	}
	
	public void drawLayer(Graphics g, AffineTransform transform) {
		if( context == null) {
			MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.");
			return;
		}
		InternalImage ii = context.getData(id);
		
		if( ii == null) return;

		Graphics2D g2  =(Graphics2D)g;
		AffineTransform prev= g2.getTransform();
		if (transform == null)
			transform = new AffineTransform();
		if( ii instanceof DynamicInternalImage) {
			transform.translate(
					((DynamicInternalImage) ii).ox, ((DynamicInternalImage) ii).oy);
		}
		
		if( context.getRenderEngine().getCompositeLayer(this) == null) {
			g2.transform(transform);
			g.drawImage( ii.cachedImage.access(), 0, 0, null);
			g2.setTransform(prev);
		}
		else {
			if( ii instanceof DynamicInternalImage) {
				g2.setTransform(new AffineTransform());
				g.drawImage( context.getRenderEngine().getCompositeLayer(this), 0, 0,  null);
			}
			else {
				g2.transform(transform);
				g.drawImage( context.getRenderEngine().getCompositeLayer(this), 0, 0,  null);
				g2.setTransform(prev);
			}
		}
	}
	
	public int getID() {
		return id;
	}

	public int getWidth() {
		return context.getWidthOf(id);
	}
	public int getHeight() {
		return context.getHeightOf(id);
	}
	
	public void refresh() {
		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = context;
		evt.dataChanged.add(this);
		context.triggerImageRefresh(evt);
	}
	
	void flush() {
		context.getData(id).cachedImage.flush();
	}
}
