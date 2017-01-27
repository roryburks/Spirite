package spirite.panel_layers;

import java.awt.datatransfer.Transferable;
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
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
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


		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getRootNode();

		constructFromRoot();
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
	
	// :::: MImageStructureObserver inherited from NodeTree
	@Override
	public void structureChanged(StructureChange evt) {
		super.structureChanged(evt);

		if( evt.isGroupTreeChange()) {
			constructFromRoot();
		}
	}
	
	// :::: WorkspaceObserver inherited from NodeTree
	@Override
	public void currentWorkspaceChanged(ImageWorkspace current, ImageWorkspace previous) {
		super.currentWorkspaceChanged(current, previous);

		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getRootNode();
		constructFromRoot();
	}
	

	// :::: ContentTree
	@Override
	protected boolean importAbove( Transferable trans, TreePath path) {
		try {
			workspace.moveAbove( nodeFromTransfer(trans), nodeFromPath(path));
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importBelow( Transferable trans, TreePath path) {
		try {
			workspace.moveBelow(nodeFromTransfer(trans), nodeFromPath(path));
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importInto( Transferable trans, TreePath path, boolean top) {
		try {
			workspace.moveInto(nodeFromTransfer(trans), (GroupNode)nodeFromPath(path), top);
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importOut( Transferable trans) {
			try {
				workspace.moveInto(nodeFromTransfer(trans), nodeRoot, false);
				return true;
			} catch (Exception e) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
				return false;
			}
	}
	
	@Override
	protected void buttonPressed(CCButton button) {
		super.buttonPressed(button);
		
		if( button.buttonNum == 1) {

			Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			if( button.isSelected())
				workspace.addToggle(node);
			else
				workspace.remToggle(node);
		}
	}
}

