package spirite.image_data;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import spirite.image_data.ImageWorkspace.ImageChangeEvent;

/**
 * Under normal circumstances an ImageHandle is a logical connection to a
 * CachedImage in an ImageWorkspace, but it can also serve as a placeholder
 * for resources which are in the middle of construction.
 * 
 * For example, one can create an ImageHandle with built-in BufferedImages
 * and then they can pass the ImageHandles to the ImageWorkspace and it'll
 * convert those BufferedImages into CachedImages, attempting to preserve
 * the id if possible.
 *
 */
public class ImageHandle {

	// Should really be a nested class, but ImageWorkspace is a bit too busy
	ImageWorkspace context;
	
	int id = -1;
	boolean locked = false;	// Mostly unused for now
	
	// workspace.getData should have constant-time access (being implemented
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
	
/*	public ImageHandle( BufferedImage img, int id, ImageWorkspace context) {
		this.context = context;
		data = context.getCacheManager().cacheImage(img, context);
		this.id = id;
	}
	
	public ImageHandle( int width, int height, Color bg, ImageWorkspace context) {
		this.context = context;
		
		data = context.getCacheManager().cacheImage(
				new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB), context);
		
        Graphics2D g2d = data.access().createGraphics();
        g2d.setColor( bg);
        g2d.fillRect( 0, 0, width, height);
        g2d.dispose();
	}*/
	
	
	@Override
	public boolean equals(Object obj) {
		if( !( obj instanceof ImageHandle))
			return false;
		ImageHandle other = (ImageHandle)obj;
		
		return (this.context == other.context) && (this.id == other.id);
	}
	
	public ImageWorkspace getContext() {
		// This method existing gives me a bad taste.  Perhaps I should rethink
		//	ImageData
		return context;
	}
	
	/** Should only be used for reading/copying.  If used for writing will not
	 * be tracked by Observers/UndoEngine. */
	public BufferedImage deepAccess() {
		return context.getData(id).access();
	}
	
	public void drawLayer(Graphics g) {
		if( context != null && context.getDrawEngine().getStrokeContext() == this) {
			BufferedImage bi = context.getDrawEngine().getStrokeEngine().getCompositionLayer();
			Graphics big = bi.getGraphics();
			big.drawImage( context.getData(id).access(), 0, 0, null);
			context.getDrawEngine().getStrokeEngine().drawStrokeLayer(big);
			g.drawImage( bi, 0, 0, null);
			big.dispose();
		}
		else {
			g.drawImage( context.getData(id).access(), 0, 0, null);
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
