package spirite.panel_layers;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.EventObject;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageStructureObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.ui.ContentTree;

public class LayerTreePanel extends ContentTree 
	implements MImageStructureObserver, MWorkspaceObserver,
	 TreeSelectionListener, TreeExpansionListener, KeyListener
{
	MasterControl master;
	ImageWorkspace workspace;
	LTPCellEditor editor;
	LTPCellRenderer renderer;
	

	// :::: Initialize
	public LayerTreePanel( MasterControl master) {
		super();
		
		
		this.master = master;
		workspace = master.getCurrentWorkspace();
		workspace.addImageStructureObserver(this);
		master.addWorkspaceObserver(this);
		
		constructFromWorkspace();
		
		renderer = new LTPCellRenderer();
		editor = new LTPCellEditor(tree, renderer);
		
		tree.setCellRenderer(renderer);
		tree.setCellEditor(editor);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
		this.addKeyListener(this);
		
		tree.setEditable(true);
//		tree.click
		
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
	public void structureChanged( StructureChangeEvent evt) {
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
			GroupTree.Node node = (GroupTree.Node)tree_node.getUserObject();
    		if( node.isExpanded() ) {
    			TreePath path =  new TreePath(tree_node.getPath());
    			tree.expandPath(path);
    		}
    		else {}
    		
    		if( node == workspace.getSelectedNode()) {
    			TreePath path =  new TreePath(tree_node.getPath());
    			tree.addSelectionPath(path);
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
		
		workspace.setSelectedNode(node);
		
		// !!! TODO Debug: this shouldn't need to be here (it should just be in ContentTree)
		repaint();
		
	}

	// :::: TreeCellRenderer


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

	// KeyListener
	@Override
	public void keyPressed(KeyEvent evt) {
		if( evt.getKeyCode() == KeyEvent.VK_F2 && getSelectedNode() != null) {
			tree.startEditingAtPath(tree.getSelectionPath());
		}
	}

	@Override	public void keyReleased(KeyEvent arg0) {}
	@Override	public void keyTyped(KeyEvent arg0) {}
	
	/***
	 * TreeCellRender
	 *
	 */
	private class LTPCellRenderer extends DefaultTreeCellRenderer {
		LayerTreeNodePanel renderPanel;
		
		LTPCellRenderer() {
			renderPanel = new LayerTreeNodePanel();
		}
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, 
				boolean expanded, boolean leaf, int row, boolean hasFocus) 
		{
			
			
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
	}

	private class LTPCellEditor extends DefaultTreeCellEditor
		implements KeyListener 
	{
		LayerTreeNodePanel renderPanel;
		String text;
		GroupTree.Node editingNode = null;
		
		public LTPCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
			super(tree, renderer);
			renderPanel = new LayerTreeNodePanel();
			renderPanel.label.addKeyListener(this);
		}
		
		
		@Override
		public Component getTreeCellEditorComponent(
				JTree tree, Object value, boolean isSelected, boolean expanded,
				boolean leaf, int row) 
		{
			Object obj = ((DefaultMutableTreeNode)value).getUserObject();

			if( obj instanceof GroupTree.GroupNode) {
				GroupTree.GroupNode node = (GroupTree.GroupNode)obj;
				editingNode = node;
				renderPanel.label.setText( node.getName());
			}
			else if( obj instanceof GroupTree.LayerNode) {
				GroupTree.LayerNode node = (GroupTree.LayerNode)obj;
				editingNode = node;
				renderPanel.label.setText( node.getLayer().getName());
				
			}
		
			return renderPanel;
		}
		
		@Override
		public void cancelCellEditing() {
			saveText();
			super.cancelCellEditing();
		}
		
		@Override
		public boolean stopCellEditing() {
			saveText();
			return super.stopCellEditing();
		}
		
		private void saveText() {
			if( editingNode != null) {
				String text = renderPanel.label.getText();
				
				editingNode.setName(text);
				
			}
			
			editingNode = null;
		}
		
		@Override
		public boolean isCellEditable(EventObject evt) {
			if( evt == null)
				return true;
			return false;
		}


		// :::: KeyListener
		@Override
		public void keyPressed(KeyEvent e) {
			if( e.getKeyCode() == KeyEvent.VK_ENTER) {
				this.stopCellEditing();
			}
		}
		@Override		public void keyReleased(KeyEvent e) {}
		@Override		public void keyTyped(KeyEvent e) {}
	}

}


