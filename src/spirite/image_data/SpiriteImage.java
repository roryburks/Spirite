package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SpiriteImage {
	List<Part> parts;
	List<Rig> rigs;
	
	int width, height;
	
	public SpiriteImage( int w, int h, Color bg) {
		parts = new ArrayList();
		
		parts.add( new Part(w, h, "base", bg));
		width = w;
		height = h;
	}
	
	public Part getActivePart() {
		return parts.get(0);
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public static class Rig {
		private List<Rig> subrigs;
		private List<Part> parts;
		int cx, cy;		// Translation X/Y
		float rx;		// Rotation X/Y
		float sx, sy;	// Scale X/Y
	}
	
}
