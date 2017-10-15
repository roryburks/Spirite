package spirite.base.graphics;

/**
 * RawImage is a wrapper for multiple different types of more-native image formats
 *
 * Created by Rory Burks on 4/29/2017.
 */

public abstract class RawImage {
	/** Gets the Width of the underlying image. */
    public abstract int getWidth();
    
    /** Gets the Height of the underlying image. */
    public abstract int getHeight();
    
    /** If true, the image is oriented with the Y-axis going upward, with
     *  0,0 at the bottom-left.  If false, the image is oriented with the
     *  Y-axis goind downward, with 0,0 at the top-left.
     */
    public abstract boolean isGLOriented();
    
    /** Flushes the image, marking it as no longer being used, allowing it to 
     * free up underlying resources.
     * 
     * NOTE: it is not guaranteed that flush will ever be called, so if the 
     * image is using native resources that need to be de-allocated, be sure
     * to override finalize.
     */
    public abstract void flush();
    
    /** Gets the amount of Bytes that the RawImage is using (generally 
     * width*height*bytesPerPixel).  This should only be used to try and 
     * approximate memory usage for memory management/UI feedback.*/
    public abstract int getByteSize();
    
    /** Creates a duplicate of this image that can be modified without altering
     * the original.
     */
    public abstract RawImage deepCopy();
    
    /** Gets the GraphicsContext for writing to the image. */
    public abstract GraphicsContext getGraphics();
    
    /**
     *  Gets the Color data at the given point in nonGL, top-to-bottom format
     *  (point 0,0 would be the top left).
     *  
     * @param x X coordinate
     * @param y Y coordinate (top-to-bottom format)
     * @return an integer packed in ARGB form
     * <li>bits 24-31: Alpha
     * <li>bits 16-23: Red
     * <li>bits 8-15: Green
     * <li>bits 0-7: Blue
     */
	public abstract int getRGB(int x, int y);
	
}
