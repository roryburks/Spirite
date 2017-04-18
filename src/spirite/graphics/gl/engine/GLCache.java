package spirite.graphics.gl.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.opengl.GL2;

import spirite.brains.MasterControl;
import spirite.graphics.gl.engine.GLEngine.PreparedTexture;
import spirite.graphics.gl.engine.GLParameters.GLTexture;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;

/** 
 * 
__ Stores a loaded texture object as required by ImageHandles
__ ImageWorkspace ties checkins/checkouts directly to GLCache so that only textures whose data has changed get deleted.
__GLParameters.GLHandleTexture accesses from GLCache, GLCache will either create new or retrieves them from memory
__Any time ImageHandles are deleted, the corresponding cache gets destroyed (even if undo actions could presumably recreate them)
__ Any time MAX_SIZE is hit, it unloads the least-recently used textures.

 * @author Rory Burks
 *
 */
public class GLCache implements MImageObserver {
	private int MAX_CACHE = 4*1024*1024;	// 4 Gig
	private GLEngine glEngine = null;
	private final MasterControl master;
	
	private class CachedTexture {
		long lastUsed;
		final PreparedTexture tex;
		boolean locked = false;
		
		CachedTexture( PreparedTexture tex) {
			this.tex = tex;
			lastUsed = System.currentTimeMillis();
		}
	}
	
	private final Map<ImageHandle, CachedTexture> cache = new HashMap<>();
	
	public GLCache(MasterControl master) {
		this.master = master;
		master.addGlobalImageObserver( this);
	}

	/** Getter that makes sure glEngine is only loaded if it's needed */
	private GLEngine getEngine() { 
		if( glEngine == null)
			glEngine = GLEngine.getInstance();
		return glEngine;
	}
	
	private CachedTexture accessCache( ImageHandle handle, GL2 gl) {
		CachedTexture ctex = cache.get(handle);
		if( ctex == null) {
			ctex = new CachedTexture(
					getEngine().prepareTexture(handle.deepAccess(), gl));
			cache.put(handle, ctex);
		}
		return ctex;
	}

	public class GLHandleTexture extends GLTexture {
		private final ImageHandle handle;
		CachedTexture ctex;
		
		public GLHandleTexture( ImageHandle handle) {
			this.handle = handle;
		}

		@Override
		public int load(GL2 gl) {
			System.out.println(handle.getID());
			ctex = accessCache(handle, gl);
			ctex.locked = true;
			return ctex.tex.getTexID();
		}

		@Override
		public void unload() {
			ctex.locked = false;
			ctex = null;
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
			ctex.tex.free();
			cache.remove(handle);
		}
	}
}
