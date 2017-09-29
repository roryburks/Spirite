package spirite.base.brains;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import spirite.base.image_data.RawImage;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

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
 * TODO: Figure out if the CacheManager is really necessary.  The RenderEngine is the 
 * only thing that really uses it as intended, all other use cases should be handled
 * by regular Garbage Collection.  It DOES keep track of memory usage so that UndoEngine
 * can remove undos once memory usage gets too big, but that can be done with substantially
 * less code and less scopes.
 * 
 * @author Rory Burks
 *
 */
public class CacheManager {
	protected long cacheSize = 0;
	private final Map<Object,CacheDomain> cache = new WeakHashMap<>();
	
	public long getCacheSize() {
		return cacheSize;
	}
	
	public final Map<Object,CacheDomain> _debugGetMap() {
		if( !MDebug.DEBUG) {
			return null;
		}
		return cache;
	}
	
	/**Removes Caches which are empty (have no CacheImages or are the 
	 * domain of an empty Reference).	 */
	public void clearUnusedDomains() {
		Iterator<Entry<Object, CacheDomain>> itOut = cache.entrySet().iterator();
		while(itOut.hasNext()) {
			CacheDomain domain = itOut.next().getValue();
			
			Iterator<CachedImage> itIn = domain.list.iterator();
			while( itIn.hasNext()) {
				CachedImage image = itIn.next();
				image._checkForCull();
				if( image.isNull())
					itIn.remove();
			}
			
			if( domain.list.isEmpty() || domain.context.get() == null) {
				//cacheSize -= domain.getSize();
				domain.flush();
				itOut.remove();
			}
		}
	}
	
	/** CacheDomains exist primarily for visibility purposes, so the user or the
	 * debugger can see which object is using which CachedImage.
	 */
	public class CacheDomain {
		private final WeakReference<Object> context;
		private final List<CachedImage> list = new LinkedList<>();
		private CacheDomain( Object context) {
			this.context = new WeakReference<>(context);
		}
		public int getSize() {
			int size = 0;
			
			for( CachedImage img : list) {
				size += img.data.getByteSize();
			}
			return size;
		}
		private void flush() {
			for( CachedImage ci : list) {
				ci.relinquishSoft(context.get());
			}
		}
	}
	
	
	public class CachedImage {
		protected RawImage data = null;
		protected long last_used;
		protected Collection<WeakReference<Object>> users = new LinkedHashSet<>();
		Object domain;
		
		CachedImage(Object domain) {
			last_used = System.currentTimeMillis();
			this.domain = domain;
			
			CacheDomain context = cache.get(domain);
			
			if( context == null) {
				context = new CacheDomain(domain);
				cache.put(domain, context);
			}
			context.list.add(this);
			reserve(domain);
		}
		
		public RawImage access() {
			last_used = System.currentTimeMillis();
			return data;
		}
		
		public void replace( RawImage bi) {
			//if( data != null)
			//	data.flush();
			cacheSize -= data.getByteSize();
			data = bi;
			cacheSize += data.getByteSize();
		}
		
		public void flush() {
			if( data == null) return;
			
			cacheSize -= data.getByteSize();
			data.flush();
			data = null;
			
			for( CacheDomain context : cache.values()) {
				int i = context.list.indexOf(this);
				if( i != -1) context.list.remove(i);
			}
			clearUnusedDomains();
		}
		
		void setData( RawImage image) {
			assert( image != null);
			if( data != null)
				cacheSize -= data.getByteSize();
			data = image;
			cacheSize += data.getByteSize();
		}
		
		public boolean isNull() {
			return data == null;
		}
		
		public void reserve( Object obj) {
			for( WeakReference<Object> user : users) {
				if( user.get() == obj)
					return;
			}
			users.add(new WeakReference<>(obj));
		}
		public void relinquish( Object obj) {
			boolean removed = false;

			Iterator<WeakReference<Object>> it = users.iterator();
			while( it.hasNext()) {
				Object user = it.next().get();
				if( user == null || user == obj) {
					it.remove();
					removed = true;
				}
			}
			if( !removed) {
				MDebug.handleError( ErrorType.STRUCTURAL, "Tried to relinquish from a non-reserved object (this probably means the intended relinquish will never happen).");
			}
			
			if( users.isEmpty()) {
				flush();
			}
		}
		
		private void _checkForCull() {
			Iterator<WeakReference<Object>> it = users.iterator();
			while( it.hasNext()) {
				Object user = it.next().get();
				if( user == null )
					it.remove();
			}
			
			if( users.isEmpty()) {
				flush();
			}
		}
		
		/** Relinquishes without giving an error if it wasn't registered;
		 * used when a Weak Reference domain has been cleared.
		 */
		private void relinquishSoft( Object obj) {
			
			// Note: Assumes that if one WeakReference of a certain Object has been
			//	cleared by GC then all others will as well.
			Iterator<WeakReference<Object>> it = users.iterator();
			while( it.hasNext()) {
				Object user = it.next().get();
				if( user == null || user == obj)
					it.remove();
			}
			if( users.isEmpty()) {
				flush();
			}
		}
	}
	
	/** Put an existing image into the Cache. */
	public CachedImage cacheImage( RawImage image, Object domain) {
		assert( image != null);
//		if( image == null) {
//			image = new BufferedImage(1,1,Globals.BI_FORMAT);
//			MUtil.clearImage(image);
//		}
		CachedImage c = new CachedImage(domain);
		c.setData(image);
		
		return c;
	}
}
