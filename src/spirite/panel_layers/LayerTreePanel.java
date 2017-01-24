package spirite.panel_layers;

import java.awt.event.ActionListener;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.ImageWorkspace.StructureChange;

public class LayerTreePanel extends NodeTree 
	implements MImageObserver, MWorkspaceObserver, MSelectionObserver,
	 TreeSelectionListener, TreeExpansionListener, ActionListener
{
	private final LayersPanel context;
	public LayerTreePanel(MasterControl master, LayersPanel context) {
		super(master);
		this.context = context;

		constructFromWorkspace();
	}

	// :::: TreeSelectionListener inherited from ContentTree
	@Override
	public void valueChanged(TreeSelectionEvent evt) {
		super.valueChanged(evt);
		
		// Called whenever the user has selected a new tree node, updates the 
		//	 Workspace so that the  active part (the part that gets drawn on) 
		//	 is changed.
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		
		if( workspace != null)
			workspace.setSelectedNode(node);
		if( context != null)
			context.updateSelected();
	}
	
	private void constructFromWorkspace() {
		if( workspace != null)
			super.constructFromNode(workspace.getRootNode());
	}
	
	// :::: MImageStructureObserver inherited from NodeTree
	@Override
	public void structureChanged(StructureChange evt) {
		super.structureChanged(evt);

		if( evt.isGroupTreeChange()) {
			constructFromWorkspace();
		}
	}
	
	// :::: WorkspaceObserver inherited from NodeTree
	@Override
	public void currentWorkspaceChanged(ImageWorkspace current, ImageWorkspace previous) {
		super.currentWorkspaceChanged(current, previous);

		constructFromWorkspace();
	}
	

}

