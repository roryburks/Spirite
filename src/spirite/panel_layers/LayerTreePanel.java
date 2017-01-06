package spirite.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageStructureObserver;
import spirite.ui.ContentTree;

public class LayerTreePanel extends ContentTree 
	implements MImageStructureObserver, MWorkspaceObserver,
	 TreeCellRenderer, TreeSelectionListener, TreeExpansionListener
{
	MasterControl master;
	ImageWorkspace workspace;
	LayerTreeNodePanel renderPanel;

	// :::: Initialize
	public LayerTreePanel( MasterControl master) {
		super();
		
		renderPanel = new LayerTreeNodePanel();
		
		this.master = master;
		workspace = master.getCurrentWorkspace();
		workspace.addImageStructureObserver(this);
		master.addWorkspaceObserver(this);
		
		constructFromWorkspace();
		
		tree.setCellRenderer(this);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
	}

    // :::: Paint
    @Override
    public void paint( Graphics g) {
    	super.paint(g);
    }
    
    // :::: API
    public GroupTree.Node getSelectedNode() {
    	return getNodeFromPath( tree.getSelectionPath());
    }
    
    private GroupTree.Node getNodeFromPath( TreePath path) {
    	if( path != null) {
    		try {
	    		return (GroupTree.Node)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
	    	}catch (ClassCastException c) {
	    		MDebug.handleWarning( WarningType.STRUCTURAL, this, "Tree Node isn't a GroupTree Node");
	    	}
    	}

		return null;
    }
    
    // :::: MImageStructureObserver interface
	@Override
	public void structureChanged() {
		constructFromWorkspace();
	}
	

	/***
	 * Called any time the structure of the image has changed, completely removes
	 * the existing tree structure and recreates it from the GroupTree data.
	 */
	private void constructFromWorkspace() {
		root.removeAllChildren();
		
		GroupTree.Node node = workspace.getRootNode();

		// Start the recursive tree traversal
		_cfw_construcRecursively( node, root);
		
		model.nodeStructureChanged(root);

		_cfw_setExpandedStateRecursively( (DefaultMutableTreeNode)model.getRoot());
		repaint();
		
		dragManager.stopDragging();
	}
	private void _cfw_construcRecursively( GroupTree.Node group_node, DefaultMutableTreeNode tree_node) {
		for( GroupTree.Node child : group_node.getChildren()) {
			DefaultMutableTreeNode node_to_add = new DefaultMutableTreeNode(child);
			
			tree_node.add( node_to_add);
			
			_cfw_construcRecursively( child, node_to_add);
		}
		
		tree_node.setAllowsChildren( group_node instanceof GroupTree.GroupNode);
	}
	private void _cfw_setExpandedStateRecursively( DefaultMutableTreeNode tree_node) {
		for( Enumeration<TreeNode> e = tree_node.children(); e.hasMoreElements();) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
			_cfw_setExpandedStateRecursively( child);
		}
		
		try {
    		if( ((GroupTree.Node)tree_node.getUserObject()).isExpanded() ) {
    			TreePath path =  new TreePath(tree_node.getPath());
    			tree.expandPath(path);
    		}
    		else {
    		}
		} catch( ClassCastException e) {}
	}
    
	// :::: DDTree
	@Override
	protected void moveAbove( TreePath nodeMove, TreePath nodeInto) {
		try {
			workspace.moveAbove(
					getNodeFromPath( nodeMove),
					getNodeFromPath( nodeInto));
		}catch (NullPointerException e) {
			MDebug.handleWarning( WarningType.STRUCTURAL, this, "Error Moving Node in Tree.");
		}
	}
	@Override
	protected void moveBelow( TreePath nodeMove, TreePath nodeInto) {
		try {
			workspace.moveBelow(
					getNodeFromPath( nodeMove),
					getNodeFromPath( nodeInto));
		}catch (NullPointerException e) {
			MDebug.handleWarning( WarningType.STRUCTURAL, this, "Error Moving Node in Tree.");
		}
	}

	@Override
	protected void moveInto( TreePath nodeMove, TreePath nodeInto, boolean top) {
		try {
			workspace.moveInto(
					getNodeFromPath( nodeMove),
					(GroupNode) getNodeFromPath( nodeInto),
					top);
		}catch (NullPointerException|ClassCastException e) {
			MDebug.handleWarning( WarningType.STRUCTURAL, this, "Error Moving Node in Tree.");
		}
	}	
	
	@Override
	protected void buttonPressed(CCButton button) {
		GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());

		node.setVisible( button.isSelected());
		master.refreshImage();
	}
	
	@Override
	protected void buttonCreated(CCButton button) {
		GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());

		button.setSelected( node.isVisible());
		
	}

	// :::: TreeExpansionListener
	@Override
	public void treeCollapsed(TreeExpansionEvent evt) {
		// Store the Expanded state in the GroupTree so that the data remembers the UI state
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		if( node != null)
			node.setExpanded(false);
	}

	@Override
	public void treeExpanded(TreeExpansionEvent evt) {
		// Store the Expanded state in the GroupTree so that the data remembers the UI state
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		if( node != null)
			node.setExpanded(true);
		
	}

	// :::: TreeSelectionListener
	/***
	 * Called whenever the user has selected a new tree node, updates the Workspace so that the 
	 * active part (the part that gets drawn on) is changed
	 */
	@Override
	public void valueChanged(TreeSelectionEvent evt) {			
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		
		if( node instanceof GroupTree.LayerNode) {
			GroupTree.LayerNode rn = (GroupTree.LayerNode)node;
			
			workspace.setActiveLayer(rn.getLayer());
		}
		else
			workspace.setActiveLayer(null);
		
		// !!! TODO Debug: this shouldn't need to be here (it should just be in ContentTree)
		repaint();
		
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
			int row, boolean hasFocus) 
	{
			
		renderPanel.setPreferredSize( new Dimension( 128, Globals.getMetric("layerpanel.treenodes.max").width + 4));
		
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
		Object obj = node.getUserObject();

		// Determine what kind of data the node on the tree contains and then 
		//	alter the node visuals accordingly
		if( obj instanceof GroupTree.GroupNode) {
			GroupTree.GroupNode gn = (GroupTree.GroupNode)obj;
			
			renderPanel.label.setText(gn.getName());
			return renderPanel;
		}
		if( obj instanceof GroupTree.LayerNode) {
			GroupTree.LayerNode rn = (GroupTree.LayerNode)obj;
			

			renderPanel.label.setText(rn.getLayer().getName());
		}
		return renderPanel;
	}

	// :::: WorkspaceObserver
	@Override
	public void currentWorkspaceChanged() {
		// Remove assosciations with the old Workspace and add ones to the new
		workspace.removeImageStructureeObserver(this);
		workspace = master.getCurrentWorkspace();
		workspace.addImageStructureObserver( this);
		this.constructFromWorkspace();
	}

	@Override	public void newWorkspace() {}

}


