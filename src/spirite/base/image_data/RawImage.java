package spirite.base.image_data;

import spirite.base.graphics.GraphicsContext;

/**
 * RawImage is a wrapper for multiple different types of more-native image formats
 *
 * Created by Rory Burks on 4/29/2017.
 */

public abstract class RawImage {
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract void flush();
    public abstract int getByteSize();
    
    public abstract RawImage deepCopy();
    
    public abstract GraphicsContext getGraphics();
	public abstract int getRGB(int x, int y);
}
