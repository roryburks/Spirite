package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageData {

	private BufferedImage data;
	int id;
	
	public ImageData( BufferedImage img, int id) {
		data = img;
		this.id = id;
	}
	
	public ImageData( int width, int height, Color bg) {
		data = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
		
        Graphics2D g2d = data.createGraphics();
        g2d.setColor( bg);
        g2d.fillRect( 0, 0, width, height);
        g2d.dispose();
	}
	
	public BufferedImage getData() {
		return data;
	}
	
	public int getID() {
		return id;
	}
}
