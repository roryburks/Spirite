package spirite.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MImageObserver;
import spirite.image_data.GroupTree;
import spirite.image_data.Part;
import spirite.image_data.SpiriteImage;

public class LayerTreePanel extends JPanel 
	implements MImageObserver
{
	MasterControl master;
	LayerTree tree;

	// :::: Initialize
	public LayerTreePanel( MasterControl master) {
		this.master = master;
		initComponents();
		
		master.addImageObserver( this);
		
	}
	
	private void initComponents() {
		// Simple grid layout, fills the whole area
		this.setLayout( new GridLayout());
		tree = new LayerTree();
		this.add(tree);
		
		// Single root is invisible, but path is visible
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		
		tree.constructFromWorkspace();
	}

    // :::: Paint
    @Override
    public void paint( Graphics g) {
    	super.paint(g);
    }
    
    private class LayerTree extends JTree 
    	implements TreeCellRenderer, TreeSelectionListener
    {
    	DefaultMutableTreeNode root;
    	DefaultTreeModel model;
    	LayerTreeNodePanel render_panel;
    	
    	public LayerTree() {
    		render_panel = new LayerTreeNodePanel();
    		this.setCellRenderer( this);
    		
    		this.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
    		
    		root = new DefaultMutableTreeNode("root");
    		model = new DefaultTreeModel(root);
    		this.setModel( model);
    		
    		this.addTreeSelectionListener(this);
    		

    	}
    	
    	/***
    	 * Called any time the structure of the image has changed, completely removes
    	 * the existing tree structure and recreates it from the GroupTree data.
    	 */
    	private void constructFromWorkspace() {
    		root.removeAllChildren();
    		
    		GroupTree.Node node = master.getImageManager().getRootNode();

    		// Start the recursive tree traversal
    		_cfw_rec( node, root);
    		
    		model.nodeStructureChanged(root);
    		tree.repaint();
    	}
    	private void _cfw_rec( GroupTree.Node group_node, DefaultMutableTreeNode tree_node) {
    		System.out.println("1");
    		for( GroupTree.Node child : group_node.getChildren()) {
    			DefaultMutableTreeNode node_to_add = new DefaultMutableTreeNode(child);
    			
    			tree_node.add( node_to_add);
    			
    			_cfw_rec( child, node_to_add);
    		}
    	}

    	
    	/***
    	 * 
    	 */
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {

			if( selected)
				render_panel.setBackground(Color.RED);
			else
				render_panel.setBackground(Color.LIGHT_GRAY);
				
			render_panel.setPreferredSize( new Dimension( 128, Globals.getMetric("layerpanel.treenodes.max").width + 4));
			
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object obj = node.getUserObject();

			// Determine what kind of data the node on the tree contains and then 
			//	alter the node visuals accordingly
			if( obj instanceof GroupTree.GroupNode) {
				GroupTree.GroupNode gn = (GroupTree.GroupNode)obj;
				
				render_panel.label.setText(gn.getName());
				return render_panel;
			}
			if( obj instanceof GroupTree.RigNode) {
				GroupTree.RigNode rn = (GroupTree.RigNode)obj;
				

				render_panel.label.setText(rn.getRig().getName());
			}
			return render_panel;
		}

		/***
		 * Called whenever the user has selected a new tree node, updates the Workspace so that the 
		 * active part (the part that gets drawn on) is changed
		 */
		@Override
		public void valueChanged(TreeSelectionEvent tse) {			
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
			
			Object obj = node.getUserObject();
			
			System.out.println( obj.getClass());
			if( obj instanceof GroupTree.RigNode) {
				GroupTree.RigNode rn = (GroupTree.RigNode)obj;
				
				master.getImageManager().setActivePart(rn.getRig());
			}
			else
				master.getImageManager().setActivePart(null);
			
		}
		
    }

	@Override
	public void imageChanged() {
//		tree.constructFromWorkspace();
	}

	@Override
	public void newImage() {
		tree.constructFromWorkspace();
		
	}
}


