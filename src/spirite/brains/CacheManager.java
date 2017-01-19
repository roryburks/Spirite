package spirite.brains;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;

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
	private final Map<Object,CacheContext> cache = new HashMap<>();
	
	public long getCacheSize() {
		return cacheSize;
	}
	
	public final Map<Object,CacheContext> _debugGetMap() {
		if( !MDebug.DEBUG) {
			return null;
		}
		
		return cache;
	}
	
	public class CacheContext {
		List<CachedImage> list = new LinkedList<>();
		public int getSize() {
			int size = 0;
			
			for( CachedImage img : list) {
				size += (img.data.getWidth() * img.data.getHeight() * img.data.getColorModel().getPixelSize())/8;
			}
			return size;
		}
	}
	
	//  Not sure if this code should be here or in RenderEngine since
	//	even though many places should use CacheManager to track resource
	//	count, tracking last_used is probably only useful to RenderEngine
	public class CachedImage {
		protected BufferedImage data = null;
		protected long last_used;
		Object user;
		
		CachedImage(Object user) {
			last_used = System.currentTimeMillis();
			
			CacheContext context = cache.get(user);
			
			if( context == null) {
				context = new CacheContext();
				cache.put(user, context);
			}
			context.list.add(this);
		}
		
		public BufferedImage access() {
			last_used = System.currentTimeMillis();
			return data;
		}
		
		public void flush() {
			cacheSize -= (data.getWidth() * data.getHeight() * data.getColorModel().getPixelSize())/8;
			data.flush();
			
			for( CacheContext context : cache.values()) {
				int i = context.list.indexOf(this);
				if( i != -1) context.list.remove(i);
			}
		}
		
		void setData( BufferedImage image) {
			data = image;
			cacheSize += (data.getWidth() * data.getHeight() * data.getColorModel().getPixelSize())/8;;
		}
	}
	
	/** Creates a new empty Cached Image*/
	public CachedImage createImage( int width, int height, Object user) {
		if( width <= 0 || height <= 0)
			return null;
		
		CachedImage c = new CachedImage(user);
		c.setData(new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB));
		
		if( c.data == null) {
			MDebug.handleError(ErrorType.ALLOCATION_FAILED, this, "Failed to create Image Data.");
			return null;
		}
		
		
		return c;
	}
	
	/** Put an existing image into the Cache. */
	public CachedImage cacheImage( BufferedImage image, Object user) {
		if( image == null) {
			image = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
			MUtil.clearImage(image);
		}
		CachedImage c = new CachedImage(user);
		c.setData(image);
		
		return c;
	}
	
	/** Creates a new CachedImage by creating a verbatim copy of a given BufferedImage*/
	public CachedImage createDeepCopy( BufferedImage toCopy, Object user) {
		
		CachedImage c = new CachedImage(user);
		c.setData(new BufferedImage( 
				toCopy.getColorModel(),
				toCopy.copyData(null),
				toCopy.isAlphaPremultiplied(),
				null));
		
		
		return c;
	}
}
