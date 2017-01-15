package spirite.image_data;

import java.awt.image.BufferedImage;

/***
 * Because awt's functions are not very abstraction-friendly, I couldn't
 * 	figure out a way to actually mask a BufferedImage to be read-only
 * 	but I nonetheless want to create a ReadOnlyImage format as a 
 * 	design guideline (to make sure ImageData is properly checked out
 * 	before writing to it).
 *
 */
public class ReadOnlyImage {
	public BufferedImage image;

	public ReadOnlyImage(BufferedImage base) {
		this.image = base;
	}
	
	public int getWidth() {
		return image.getWidth();
	}
	
	public int getHeight() {
		return image.getHeight();
	}
}
