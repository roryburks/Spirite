package spirite.image_data;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;

/**
 * Under normal circumstances an ImageHandle is a logical connection to a
 * CachedImage in an ImageWorkspace.  It also helps smooth out the process
 * of building complex data structures relying on Images that should be 
 * Cached by allowing you to create ImageHandles without a null context
 * and then linking them to the ImageWorkspace using importData
 */
public class ImageHandle {
	ImageWorkspace context;
	
	int id = -1;
	boolean locked = false;	// Mostly unused for now
	
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
	
	/** Should only be used for reading/copying.  If used for writing will not
	 * be tracked by Observers/UndoEngine. */
	public BufferedImage deepAccess() {
		if( context == null) return null;
		return context.getData(id).access();
	}
	
	public void drawLayer(Graphics g) {
		CachedImage ci = context.getData(id);
		
		if( ci == null) return;
		
		if( context != null && context.getDrawEngine().getStrokeContext() == this) {
			BufferedImage bi = context.getDrawEngine().getStrokeEngine().getCompositionLayer();
			Graphics big = bi.getGraphics();
			big.drawImage( ci.access(), 0, 0, null);
			context.getDrawEngine().getStrokeEngine().drawStrokeLayer(big);
			g.drawImage( bi, 0, 0, null);
			big.dispose();
		}
		else {
			g.drawImage( ci.access(), 0, 0, null);
		}
	}
	
	public int getID() {
		return id;
	}

	public int getWidth() {
		return context.getData(id).access().getWidth();
	}
	public int getHeight() {
		return context.getData(id).access().getHeight();
	}
	
	public void refresh() {
		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = context;
		evt.dataChanged.add(this);
		context.triggerImageRefresh(evt);
	}
	
	void flush() {
		context.getData(id).flush();
	}
	
}
