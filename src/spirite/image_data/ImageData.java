package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import spirite.image_data.ImageWorkspace.ImageChangeEvent;


public class ImageData {
	// Should really be a nested class, but ImageWorkspace is a bit too busy
	private final ImageWorkspace context;
	
	
	private BufferedImage data;
	int id;
	boolean locked = false;	// Mostly unused for now
	
	public ImageData( BufferedImage img, int id, ImageWorkspace context) {
		this.context = context;
		data = img;
		this.id = id;
	}
	
	public ImageData( int width, int height, Color bg, ImageWorkspace context) {
		this.context = context;
		
		data = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
		
        Graphics2D g2d = data.createGraphics();
        g2d.setColor( bg);
        g2d.fillRect( 0, 0, width, height);
        g2d.dispose();
	}
	
	public ReadOnlyImage readImage() {
		return new ReadOnlyImage(data);
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
}
