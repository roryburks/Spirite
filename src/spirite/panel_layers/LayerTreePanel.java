package spirite.panel_layers;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.dialogs.Dialogs;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.animation_data.SimpleAnimation;
import spirite.ui.ContentTree;
import spirite.ui.UIUtil;

public class LayerTreePanel extends ContentTree 
	implements MImageObserver, MWorkspaceObserver,
	 TreeSelectionListener, TreeExpansionListener, MSelectionObserver, ActionListener
{
	// LayerTreePanel only needs master to add a WorkspaceObserver
//	MasterControl master;
	ImageWorkspace workspace;
	LayersPanel context;
	
	private static final long serialVersionUID = 1L;
	LTPCellEditor editor;
	LTPCellRenderer renderer;
	

	// :::: Initialize
	public LayerTreePanel( MasterControl master, LayersPanel context) {
		super();
		this.context = context;
		
		// Add Observers
		workspace = master.getCurrentWorkspace();
		workspace.addImageObserver(this);
		workspace.addSelectionObserver(this);
		master.addWorkspaceObserver(this);
		
		// Set Tree Properties
		renderer = new LTPCellRenderer();
		editor = new LTPCellEditor(tree, renderer);
		tree.setCellRenderer(renderer);
		tree.setCellEditor(editor);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
		tree.setEditable(true);
		setButtonsPerRow(2);

		scrollPane.setAutoscrolls(false);
		
		constructFromWorkspace();
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
    
    // :::: MImageObserver interface
    @Override    public void imageChanged() {}
	@Override
	public void structureChanged( StructureChange evt) {
		constructFromWorkspace();
	}
	
	// :::: MSelectionObserver
	@Override
	public void selectionChanged(Node newSelection) {
		TreePath path = getPathOfNode( newSelection);
		
		if(path != null)
			tree.setSelectionPath(path);
	}
	
	@SuppressWarnings("unchecked")
	private TreePath getPathOfNode( Node nodeToFind){
		TreeModel m = tree.getModel();
		
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)m.getRoot();
		Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
		
		while( e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			
			if( node.getUserObject() == nodeToFind) {
				return new TreePath(node.getPath());
			}
		}
		
		return null;
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
		
//		repaint();	// Not really necessary since nodeStructureChanged should handle it
		
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
	@SuppressWarnings("unchecked")
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
    
	// :::: ContentTree
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
		if( button.buttonNum == 0) {
			GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			workspace.setNodeVisibility(node,  button.isSelected());
		}
	}
	@Override
	protected void buttonCreated(CCButton button) {
		if( button.buttonNum == 0) {
			GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			button.setSelected( node.isVisible());
		}
	}
	@Override
	protected void clickPath(TreePath path, MouseEvent evt) {
		super.clickPath(path, evt);

		if( path == null) return;
		if( evt.getButton() == MouseEvent.BUTTON3) {
			
			DefaultMutableTreeNode node = 
					(DefaultMutableTreeNode)path.getLastPathComponent();
			
			// Construct the base Context Menu
			String descriptor = "...";
			
			Object usrObj = node.getUserObject();
			if( usrObj instanceof GroupNode) {
				descriptor = "Layer Group";
			}
			if( usrObj instanceof LayerNode) {
				descriptor = "Layer";
			}
			
			String[][] baseMenuScheme = {
					{"&New Layer", "newLayer", null},
					{"New Layer &Group", "newGroup", null},
					{"-"},
					{"Duplicate "+descriptor, "duplicate", null}, 
					{"Delete  "+descriptor, "delete", null}, 
			};
			List<String[]> menuScheme = new ArrayList<>(Arrays.asList(baseMenuScheme));
			
			// Add parts to the menu scheme depending on node type
			if( usrObj instanceof GroupNode) {
				menuScheme.add( new String[]{"-"});
				menuScheme.add( new String[]{"&Construct Simple Animation From Group", "animfromgroup", null});
			}
			else if( usrObj instanceof LayerNode) {
			}

			// Show the ContextMenu
			contextMenu.removeAll();
			UIUtil.constructMenu(contextMenu, menuScheme.toArray( new String[0][]), this);
			contextMenu.context = (Node)usrObj;
			contextMenu.show(this, evt.getX(), evt.getY());
		}
	}
	
	LTPContextMenu contextMenu = new LTPContextMenu();
	class LTPContextMenu extends JPopupMenu {
		Node context = null;
		
		public LTPContextMenu() {}
	}

	// :::: TreeExpansionListener
	@Override
	public void treeCollapsed(TreeExpansionEvent evt) {
		super.treeCollapsed(evt);
		
		// Store the Expanded state in the GroupTree so that the data remembers the UI state
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		if( node != null)
			node.setExpanded(false);
	}
	@Override
	public void treeExpanded(TreeExpansionEvent evt) {
		super.treeExpanded(evt);
		
		// Store the Expanded state in the GroupTree so that the data remembers the UI state
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		if( node != null)
			node.setExpanded(true);
		
	}

	// :::: TreeSelectionListener
	@Override
	public void valueChanged(TreeSelectionEvent evt) {
		super.valueChanged(evt);
		
		// Called whenever the user has selected a new tree node, updates the 
		//	 Workspace so that the  active part (the part that gets drawn on) 
		//	 is changed.
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		
		workspace.setSelectedNode(node);
		
		// !!! TODO Debug: this shouldn't need to be here (it should just be in ContentTree)
		
		context.opacitySlider.repaint();
		repaint();
		
	}
	

	// :::: WorkspaceObserver
	@Override
	public void currentWorkspaceChanged( ImageWorkspace current, ImageWorkspace previous) {
		// Remove assosciations with the old Workspace and add ones to the new
		workspace.removeImageObserver(this);
		workspace.removeSelectionObserver(this);
		workspace = current;
		workspace.addImageObserver( this);
		workspace.addSelectionObserver(this);
		this.constructFromWorkspace();
	}
	@Override	public void newWorkspace( ImageWorkspace arg0) {}
	@Override	public void removeWorkspace( ImageWorkspace arg0) {}
	

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		// ActionCommands from JPopupMenu
		if( evt.getActionCommand().equals("animfromgroup")) {
			GroupNode group = (GroupNode)contextMenu.context;
			workspace.getAnimationManager().addAnimation(new SimpleAnimation(group));
		}
		else if (evt.getActionCommand().equals("newGroup")){
			workspace.addGroupNode(contextMenu.context, "New Group");
		}
		else if (evt.getActionCommand().equals("newLayer")) {
			Dialogs.performNewLayerDialog(workspace);
		}
		else if (evt.getActionCommand().equals("duplicate")) {
			
		}
		else if (evt.getActionCommand().equals("delete")) {
			workspace.removeNode(contextMenu.context);
		}
		else {
			System.out.println(evt.getActionCommand());
		}
	}
	
	/***
	 * TreeCellRender
	 *
	 */
	private class LTPCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;
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
			if( obj instanceof GroupTree.Node) {
				String str = ((GroupTree.Node)obj).getName();
				if( MDebug.DEBUG && obj instanceof GroupTree.LayerNode)
					str += " " + ((GroupTree.LayerNode)obj).getImageData().getID();
				renderPanel.label.setText( str);
			}
			
			if( obj instanceof GroupTree.LayerNode) {
				renderPanel.ppanel.image = ((GroupTree.LayerNode)obj).getImageData();
			}
			else
				renderPanel.ppanel.image = null;
		
			return renderPanel;
		}
	}

	/***
	 * Tree Cell Editor
	 */
	private class LTPCellEditor extends DefaultTreeCellEditor
		implements KeyListener 
	{
		LayerTreeNodePanel renderPanel;
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

			// Determine what kind of data the node on the tree contains and then 
			//	alter the node visuals accordingly
			if( obj instanceof GroupTree.Node) {
				editingNode = (GroupTree.Node)obj;
				renderPanel.label.setText( editingNode.getName());
			}

			if( obj instanceof GroupTree.LayerNode) {
				renderPanel.ppanel.image = ((GroupTree.LayerNode)obj).getImageData();
			}
			else
				renderPanel.ppanel.image = null;
		
			return renderPanel;
		}
		
		@Override
		public void cancelCellEditing() {
			super.cancelCellEditing();
			saveText();
		}
		
		@Override
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
		
		private void saveText() {
			if( editingNode != null) {
				String text = renderPanel.label.getText();

				workspace.renameNode(editingNode, text);
				
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
				this.cancelCellEditing();
			}
		}
		@Override		public void keyReleased(KeyEvent e) {}
		@Override		public void keyTyped(KeyEvent e) {}
	}


	

}


