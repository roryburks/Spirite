package spirite.base.graphics;

/**
 * RawImage is a wrapper for multiple different types of more-native image formats
 *
 * Created by Rory Burks on 4/29/2017.
 */

public interface RawImage extends IImage  {
	public static class InvalidImageDimensionsExeption extends Exception {
		public InvalidImageDimensionsExeption( String message) {super(message);}
	}
	
    /** Gets the GraphicsContext for writing to the image. */
    public abstract GraphicsContext getGraphics();
}
