package spirite.image_data.layers;

import java.awt.Graphics;
import java.util.List;

import spirite.image_data.ImageData;

public abstract class Layer {
	public abstract ImageData getActiveData();
	public abstract List<ImageData> getUsedImageData();
	public abstract void draw( Graphics g);
	public abstract int getWidth();
	public abstract int getHeight();
	
	public abstract Layer duplicate();
}
