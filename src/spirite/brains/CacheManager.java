package spirite.brains;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
 * domain - The "area" of the program in which the CachedImage exists, in general.
 * 		This is used primarily for debug/user-visibility reasons.
 * user - An object which is using the data.  By default these aren't tracked
 * 		but if a CachedImage has multiple users, they can use the user tracking
 * 		features to automatically flush the data if all users relinquish it
 * 		(alternately anything that has access to the CachedImage can flush it manually
 * 		even if other users are using it).
 * 		
 * 
 * @author Rory Burks
 *
 */
public class CacheManager {
	protected long cacheSize = 0;
	private final Map<Object,CacheDomain> cache = new HashMap<>();
	
	public long getCacheSize() {
		return cacheSize;
	}
	
	public final Map<Object,CacheDomain> _debugGetMap() {
		if( !MDebug.DEBUG) {
			return null;
		}
		
		return cache;
	}
	
	public class CacheDomain {
		private final Object context;
		private CacheDomain( Object context) {
			this.context = context;
		}
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
		protected Collection<Object> users = new LinkedHashSet<>();
		Object domain;
		
		CachedImage(Object domain) {
			last_used = System.currentTimeMillis();
			
			CacheDomain context = cache.get(domain);
			
			if( context == null) {
				context = new CacheDomain(domain);
				cache.put(domain, context);
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
			
			List<CacheDomain> toRem = new ArrayList<CacheDomain>();
			for( CacheDomain context : cache.values()) {
				int i = context.list.indexOf(this);
				if( i != -1) context.list.remove(i);
				
				if( context.list.isEmpty()) {
					toRem.add(context);
				}
			}
			for( CacheDomain rem : toRem) {
				cache.remove(rem.context);
			}
			
		}
		
		void setData( BufferedImage image) {
			data = image;
			cacheSize += (data.getWidth() * data.getHeight() * data.getColorModel().getPixelSize())/8;;
		}
		
		public void reserve( Object obj) {
			users.add(obj);
		}
		public void relinquish( Object obj) {
			if( !users.contains(obj)) {
				MDebug.handleError( ErrorType.STRUCTURAL, this, "Tried to relinquish from a non-reserved object (this probably means the intended relinquish will never happen).");
			}
			
			users.remove(obj);
			if( users.isEmpty()) {
				flush();
			}
		}
	}
	
	/** Creates a new empty Cached Image*/
	public CachedImage createImage( int width, int height, Object domain) {
		if( width <= 0 || height <= 0)
			return null;
		
		CachedImage c = new CachedImage(domain);
		c.setData(new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB));
		
		if( c.data == null) {
			MDebug.handleError(ErrorType.ALLOCATION_FAILED, this, "Failed to create Image Data.");
			return null;
		}
		
		
		return c;
	}
	
	/** Put an existing image into the Cache. */
	public CachedImage cacheImage( BufferedImage image, Object domain) {
		if( image == null) {
			image = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
			MUtil.clearImage(image);
		}
		CachedImage c = new CachedImage(domain);
		c.setData(image);
		
		return c;
	}
	
	/** Creates a new CachedImage by creating a verbatim copy of a given BufferedImage*/
	public CachedImage createDeepCopy( BufferedImage toCopy, Object domain) {
		
		CachedImage c = new CachedImage(domain);
		c.setData(new BufferedImage( 
				toCopy.getColorModel(),
				toCopy.copyData(null),
				toCopy.isAlphaPremultiplied(),
				null));
		
		
		return c;
	}
}
