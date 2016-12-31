package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Part {

	private BufferedImage data;
	private String type;
	public int cx, cy;
	
	public Part( int width, int height, String type, Color bg) {
		cx = width / 2;
		cy = height / 2;
		data = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
		this.type = type;
		
        Graphics2D g2d = data.createGraphics();
        System.out.println(bg);
        g2d.setColor( bg);
        g2d.fillRect( 0, 0, width, height);
        g2d.dispose();
	}
	
	public BufferedImage getData() {
		return data;
	}
	
	public void setType( String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}

	public int getCenterX() {
		return cx;
	}
	public int getCenterY() {
		return cy;
	}
}
