package spirite.base.graphics;

import spirite.base.image_data.ImageWorkspace;

public abstract class WorkspaceRenderer {
	public abstract void renderWorkspace( ImageWorkspace workspace, GraphicsContext gc);
	public abstract void renderReference( ImageWorkspace workspace, boolean front, GraphicsContext gc);
	
	public abstract void checkTimeout();
}
