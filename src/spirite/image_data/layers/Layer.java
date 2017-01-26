package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;

import spirite.image_data.ImageHandle;

public abstract class Layer {
	public abstract ImageHandle getActiveData();
	public abstract List<ImageHandle> getUsedImageData();
	public abstract void draw( Graphics g);
	public abstract int getWidth();
	public abstract int getHeight();
	
	/**
	 * Given a proposed Cropping region, returns a list corresponding
	 * to which areas of the ImageData it uses should be cropped.
	 */
	public abstract List<Rectangle> interpretCrop(Rectangle rect);
	
	/**
	 * Creates a logical duplicate of the Layer, creating Null-Context
	 * ImageHandles.
	 */
	public abstract Layer logicalDuplicate();
}
