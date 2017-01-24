package spirite.image_data.layers;

import java.awt.Graphics;
import java.util.List;

import spirite.image_data.ImageHandle;

public abstract class Layer {
	public abstract ImageHandle getActiveData();
	public abstract List<ImageHandle> getUsedImageData();
	public abstract void draw( Graphics g);
	public abstract int getWidth();
	public abstract int getHeight();
	
	/**
	 * Creates a logical duplicate of the Layer, creating Null-Context
	 * ImageHandles.
	 */
	public abstract Layer logicalDuplicate();
}
