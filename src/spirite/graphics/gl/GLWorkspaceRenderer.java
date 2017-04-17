package spirite.graphics.gl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.graphics.GraphicsContext;
import spirite.graphics.WorkspaceRenderer;
import spirite.graphics.gl.engine.GLMultiRenderer;
import spirite.image_data.ImageWorkspace;

public class GLWorkspaceRenderer extends WorkspaceRenderer implements MWorkspaceObserver {
	
	private class GLCache {
		List<GLMultiRenderer> glmus;
		long lastUsed;
		
		void clear() {
			for( GLMultiRenderer glmu : glmus) {
				glmu.cleanup();
			}
			glmus.clear();
		}
	}
	
	private final MasterControl master;
	public GLWorkspaceRenderer( MasterControl master) {
		this.master = master;
		
		master.addWorkspaceObserver(this);
	}
	
	private final Map<ImageWorkspace, GLCache> wsCache = new HashMap<>();

	@Override
	public void renderWorkspace(ImageWorkspace workspace, GraphicsContext gc) {
		GLGraphics glgc = (GLGraphics)gc;
		
		GLCache cache = wsCache.get(workspace);
		
		if( workspace.getDrawEngine().strokeIsDrawing()) {
			workspace.getDrawEngine().getStrokeEngine().drawStrokeLayer(gc);
		}
	}

	@Override
	public void renderReference(ImageWorkspace workspace, boolean front, GraphicsContext gc) {
		GLGraphics glgc = (GLGraphics)gc;
	}
	
	@Override
	public void checkTimeout() {
		// TODO Auto-generated method stub
		
	}

	// :::: MWorkspace Observer
	@Override public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {}
	@Override public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void removeWorkspace(ImageWorkspace remWorkspace) {
		GLCache cache = wsCache.get(remWorkspace);
		if( cache != null)
			cache.clear();	
		wsCache.remove(remWorkspace);
	}

}
