package spirite.panel_layers;

import java.awt.datatransfer.Transferable;

import javax.swing.tree.TreePath;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MReferenceObserver;

public class ReferenceTreePanel extends NodeTree 
	implements MReferenceObserver 
{
	private final ReferenceSchemePanel context;

	public ReferenceTreePanel(MasterControl master, ReferenceSchemePanel context) {
		super(master);
		this.context= context;
		
		super.constructFromNode(null);
		nodeRoot = null;
		

		if( workspace == null)
			nodeRoot = null;
		else {
			nodeRoot = workspace.getReferenceRoot();
			workspace.addReferenceObserve(this);
		}

		constructFromRoot();
		
		this.setButtonsPerRow(1);
	}
	

	// :::: WorkspaceObserver inherited from NodeTree
	@Override
	public void currentWorkspaceChanged(ImageWorkspace current, ImageWorkspace previous) {
		if( workspace != null) {
			workspace.removeReferenceObserve(this);
		}
		
		super.currentWorkspaceChanged(current, previous);

		if( workspace == null)
			nodeRoot = null;
		else {
			nodeRoot = workspace.getReferenceRoot();
			workspace.addReferenceObserve(this);
		}
		constructFromRoot();
	}

	
	// :::: ContentTree
	@Override
	protected boolean importAbove( Transferable trans, TreePath path) {
		try {
			Node tnode = nodeFromTransfer(trans);
			Node context = nodeFromPath(path);
			
			if( workspace.verifyReference(tnode)) {
				workspace.moveAbove(tnode, context);
			}
			else {
				Node toAdd = workspace.shallowDuplicateNode(tnode);
				workspace.addReferenceNode(toAdd, context.getParent(), context);
			}
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
			return false;
		}
	}
	@Override
	protected boolean importBelow( Transferable trans, TreePath path) {
		try {
			Node tnode = nodeFromTransfer(trans);
			Node context = nodeFromPath(path);
			
			if( workspace.verifyReference(tnode)) {
				workspace.moveBelow(tnode, context);
			}
			else  {
				Node toAdd = workspace.shallowDuplicateNode(tnode);
				workspace.addReferenceNode(toAdd, context.getParent(), context.getNextNode());
			}
			
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
			return false;
		}
	}
	@Override
	protected boolean importInto( Transferable trans, TreePath path, boolean top) {
		try {
			Node tnode =nodeFromTransfer(trans);
			Node context = nodeFromPath(path);
			Node before = null;

			if( workspace.verifyReference(tnode))
				workspace.moveInto(tnode, (GroupNode) context, top);
			else {	
				Node toAdd =  workspace.shallowDuplicateNode(tnode);
				if(top && !context.getChildren().isEmpty())
					before = context.getChildren().get(0);
	
				workspace.addReferenceNode(toAdd, context, before);
			}
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
			return false;
		}
	}
	@Override
	protected boolean importOut( Transferable trans) {
			try {
				Node tnode = nodeFromTransfer(trans);
				
				if( !workspace.verifyReference(tnode)) {
					Node toAdd = workspace.shallowDuplicateNode(tnode);
					workspace.addReferenceNode(toAdd, workspace.getReferenceRoot(), null);
				}
				return true;
			} catch (Exception e) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
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
	
	// Prevents a self-over from being interpreted as an importClear
	@Override
	protected boolean importSelf(Transferable trans, TreePath path) {
		return true;
	}


	// :::: MReferenceObserver
	@Override
	public void referenceStructureChanged(boolean hard) {
		if( hard)
			constructFromRoot();
		
	}


	@Override
	public void toggleReference(boolean referenceMode) {}
	
	// :::: NodeTree
	@Override
	void cleanup() {
		if( workspace != null)
			workspace.removeReferenceObserve(this);
		super.cleanup();
	}
}
