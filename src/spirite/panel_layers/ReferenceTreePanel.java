package spirite.panel_layers;

import spirite.brains.MasterControl;
import spirite.image_data.ImageWorkspace;

public class ReferenceTreePanel extends NodeTree {

	public ReferenceTreePanel(MasterControl master) {
		super(master);
		
		super.constructFromNode(null);
		constructFromWorkspace();
	}

	private void constructFromWorkspace() {
		if( workspace != null)
			super.constructFromNode(workspace.getRootNode());
	}
	

	@Override
	public void currentWorkspaceChanged(ImageWorkspace current, ImageWorkspace previous) {
		super.currentWorkspaceChanged(current, previous);

		constructFromWorkspace();
	}

}
