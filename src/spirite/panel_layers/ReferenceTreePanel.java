package spirite.panel_layers;

import java.awt.datatransfer.Transferable;

import javax.swing.tree.TreePath;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;

public class ReferenceTreePanel extends NodeTree {

	public ReferenceTreePanel(MasterControl master) {
		super(master);
		
		super.constructFromNode(null);
		nodeRoot = null;
		

		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getReferenceRoot();

		constructFromRoot();
		
		this.setButtonsPerRow(1);
	}
	

	// :::: WorkspaceObserver inherited from NodeTree
	@Override
	public void currentWorkspaceChanged(ImageWorkspace current, ImageWorkspace previous) {
		super.currentWorkspaceChanged(current, previous);

		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getReferenceRoot();
		constructFromRoot();
	}

	
	// :::: ContentTree
	@Override
	protected boolean importAbove( Transferable trans, TreePath path) {
		try {
			System.out.println("b");
			Node toAdd = workspace.shallowDuplicateNode(nodeFromTransfer(trans));
			Node context = nodeFromPath(path);
			workspace.addReferenceNode(toAdd, context.getParent(), context);
			constructFromRoot();
			System.out.println("b");
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importBelow( Transferable trans, TreePath path) {
		try {
			System.out.println("ABOVE");
			Node toAdd = workspace.shallowDuplicateNode(nodeFromTransfer(trans));
			Node context = nodeFromPath(path);
			workspace.addReferenceNode(toAdd, context.getParent(), context.getNextNode());
			constructFromRoot();
			System.out.println("ABOVE");
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importInto( Transferable trans, TreePath path, boolean top) {
		try {
			System.out.println("Into");
			Node toAdd = workspace.shallowDuplicateNode(nodeFromTransfer(trans));
			Node context = nodeFromPath(path);
			Node before = null;
			
			if(top && !context.getChildren().isEmpty())
				before = context.getChildren().get(0);

			workspace.addReferenceNode(toAdd, context, before);
			constructFromRoot();
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importOut( Transferable trans) {
			try {
				Node toAdd = workspace.shallowDuplicateNode(nodeFromTransfer(trans));
				
				workspace.addReferenceNode(toAdd, workspace.getReferenceRoot(), null);
				constructFromRoot();
				return true;
			} catch (Exception e) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
				return false;
			}
	}
	
	@Override
	protected boolean importClear(TreePath path) {
		Node node = nodeFromPath(path);
		workspace.clearReferenceNode(node);
		constructFromRoot();
		return super.importClear(path);
	}
}
