package spirite.brains;

import java.awt.image.BufferedImage;

import spirite.MDebug;
import spirite.MDebug.ErrorType;

/***
 * The CacheManager keeps track of all the various dynamic memory 
 * data allocated by the program to make sure they stay within certain
 * bounds.
 * 
 * Note: The CacheManager is by no means a definitive track of how
 * much memory the program is using.  The only thing it keeps track
 * of is an estimated amount of space used by the visual memory of
 * cached images.  This does NOT include visual memory of things like
 * Icons.
 * 
 * @author Rory Burks
 *
 */
public class CacheManager {
	protected long cacheSize = 0;
	
	public long getCacheSize() {
		return cacheSize;
	}
	
	
	//  Not sure if this code should be here or in RenderEngine since
	//	even though many places should use CacheManager to track resource
	//	count, tracking last_used is probably only useful to RenderEngine
	public class CachedImage {
		protected BufferedImage data;
		protected long last_used;
		
		CachedImage() {
			last_used = System.currentTimeMillis();
		}
		
		public BufferedImage access() {
			last_used = System.currentTimeMillis();
			return data;
		}
		
		public void flush() {
			cacheSize -= data.getWidth() * data.getHeight() * 4;
			data.flush();
		}
	}
	
	/** Creates a new empty Cached Image*/
	public CachedImage createImage( int width, int height) {
		if( width <= 0 || height <= 0)
			return null;
		
		CachedImage c = new CachedImage();
		c.data = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
		
		if( c.data == null) {
			MDebug.handleError(ErrorType.ALLOCATION_FAILED, this, "Failed to create Image Data.");
			return null;
		}
		cacheSize += 4 * width * height;
		
		
		return c;
	}
	
	/** Creates a new CachedImage by creating a verbatim copy of a given BufferedImage*/
	public CachedImage createDeepCopy( BufferedImage toCopy) {
		
		CachedImage c = new CachedImage();
		c.data = new BufferedImage( 
				toCopy.getColorModel(),
				toCopy.copyData(null),
				toCopy.isAlphaPremultiplied(),
				null);
		cacheSize += (c.data.getWidth() * c.data.getHeight() * c.data.getColorModel().getPixelSize())/8;
		
		
		return c;
	}
}
