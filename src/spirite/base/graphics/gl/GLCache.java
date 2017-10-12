package spirite.base.graphics.gl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.gl.GLParameters.GLTexture;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.hybrid.MDebug;

/** 
 * TODO:
 * GLCache is most likely out-dated and unnecessary.  It existed to cache 
 * conversions from BufferedImages into OpenGL textures so that they were
 * not done many dozen times per second.  But now use of BufferedImages
 * in GLMode have almost entirely been eliminated.
 * 
 * @author Rory Burks
 *
 */
public class GLCache implements MImageObserver {
	private long MAX_CACHE = 4294967296L;	// 4 Gig
	private long cacheSize = 0;
	private GLEngine glEngine = null;
	
	private class CachedTexture {
		long lastUsed;
		final GLImage tex;
		boolean locked = false;
		
		CachedTexture( GLImage tex) {
			this.tex = tex;
//			lastUsed = System.currentTimeMillis();	// Should be called regardless
		}
	}
	
	private final Map<ImageHandle, CachedTexture> cache = new HashMap<>();
	
	public GLCache(MasterControl master) {
		master.addGlobalImageObserver( this);
	}
	
	public long getCacheSize() {return cacheSize;}

	/** Getter that makes sure glEngine is only loaded if it's needed */
	private GLEngine getEngine() { 
		if( glEngine == null)
			glEngine = GLEngine.getInstance();
		return glEngine;
	}
	
	private CachedTexture accessCache( ImageHandle handle) {
		CachedTexture ctex = cache.get(handle);
		if( ctex == null) {
			ctex = new CachedTexture(
					getEngine().prepareTexture( handle.deepAccess()));
			
			cacheSize += ctex.tex.getByteSize();
			
			if( cacheSize > MAX_CACHE) {
				ArrayList<Entry<ImageHandle,CachedTexture>> list = new ArrayList<>(cache.entrySet());
				list.sort( new Comparator<Entry<ImageHandle,CachedTexture>>() {
					@Override public int compare(Entry<ImageHandle,CachedTexture> o1, Entry<ImageHandle,CachedTexture> o2) {
						return (int) (o1.getValue().lastUsed-o2.getValue().lastUsed);
					}
				});
				for(Entry<ImageHandle,CachedTexture> entry : list) {
					MDebug.log("GL Cache Overflow");
					if( cacheSize < MAX_CACHE) break;
					voidOutHandle(entry.getKey());
				}
			}
			
			cache.put(handle, ctex);
		}
		ctex.lastUsed = System.currentTimeMillis();
		return ctex;
	}

	public class GLHandleTexture extends GLTexture {
		private final ImageHandle handle;
		CachedTexture ctex;
		
		public GLHandleTexture( ImageHandle handle) {
			this.handle = handle;
		}

		@Override
		public int load() {
			ctex = accessCache(handle);
			ctex.locked = true;
			return ctex.tex.getTexID();
		}

		@Override
		public void unload() {
			ctex.locked = false;
			ctex = null;
		}
		@Override public int getWidth() { return handle.deepAccess().getWidth();}
		@Override public int getHeight() { return handle.deepAccess().getHeight(); }

		@Override
		public boolean isGLOriented() {
			return false;
		}
	}

	@Override
	public void imageChanged(ImageChangeEvent evt) {
		for( ImageHandle handle : evt.getChangedImages()) {
			voidOutHandle(handle);
		}
	}

	@Override
	public void structureChanged(StructureChangeEvent evt) {
		Collection<ImageHandle> dependencies = evt.change.getDependencies();
		
		if( dependencies != null) {
			for( ImageHandle handle : dependencies) {
				voidOutHandle(handle);
			}
		}
	}
	
	private void voidOutHandle(ImageHandle handle) {
		CachedTexture ctex = cache.get(handle);
		if( ctex != null && !ctex.locked) {
			if( ! (ctex.tex instanceof GLImage) )
				ctex.tex.flush();
			cacheSize -= ctex.tex.getByteSize();
			cache.remove(handle);
		}
	}
}
