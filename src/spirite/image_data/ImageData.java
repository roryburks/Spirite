package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;


public class ImageData {
	// Should really be a nested class, but ImageWorkspace is a bit too busy
	private final ImageWorkspace context;
	
	private CachedImage data;
	int id;
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
	
	public ReadOnlyImage readImage() {
		return new ReadOnlyImage(data.access());
	}
	
	public int getID() {
		return id;
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
