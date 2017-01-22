package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;


public class ImageData {

	// Should really be a nested class, but ImageWorkspace is a bit too busy
	private final ImageWorkspace context;
	
	private CachedImage data;
	int id = -1;
	boolean locked = false;	// Mostly unused for now
	
	public ImageData( BufferedImage img, int id, ImageWorkspace context) {
		this.context = context;
		data = context.getCacheManager().cacheImage(img, context);
		this.id = id;
	}
	
	public ImageData( int width, int height, Color bg, ImageWorkspace context) {
		this.context = context;
		
		data = context.getCacheManager().cacheImage(
				new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB), context);
		
        Graphics2D g2d = data.access().createGraphics();
        g2d.setColor( bg);
        g2d.fillRect( 0, 0, width, height);
        g2d.dispose();
	}
	
	public ImageWorkspace getContext() {
		// This method existing gives me a bad taste.  Perhaps I should rethink
		//	ImageData
		return context;
	}
	
	/** Should only be used for reading/copying.  If used for writing will not
	 * be tracked by Observers/UndoEngine. */
	public BufferedImage deepAccess() {
		return data.access();
	}
	
	public void drawLayer(Graphics g) {
		if( context != null && context.getDrawEngine().getStrokeContext() == this) {
			BufferedImage bi = context.getDrawEngine().getStrokeEngine().getCompositionLayer();
			Graphics big = bi.getGraphics();
			big.drawImage( data.access(), 0, 0, null);
			context.getDrawEngine().getStrokeEngine().drawStrokeLayer(big);
			g.drawImage( bi, 0, 0, null);
			big.dispose();
		}
		else {
			g.drawImage( data.access(), 0, 0, null);
		}
	}
	
	public int getID() {
		return id;
	}

	public int getWidth() {
		return data.access().getWidth();
	}
	public int getHeight() {
		return data.access().getHeight();
	}
	
	public void refresh() {
		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = context;
		evt.dataChanged.add(this);
		context.triggerImageRefresh(evt);
	}
	
	void flush() {
		data.flush();
	}
	
}
