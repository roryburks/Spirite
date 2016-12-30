package spirite.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
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
import spirite.MDebug;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MImageObserver;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace.MImageStructureObserver;
import spirite.image_data.Part;
import spirite.image_data.SpiriteImage;

public class LayerTreePanel extends JPanel 
	implements MImageStructureObserver
{
	MasterControl master;
	LayerTree tree;

	// :::: Initialize
	public LayerTreePanel( MasterControl master) {
		this.master = master;
		initComponents();
		
		master.getCurrentWorkspace().addImageStructureObserver(this);
		
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
	
	public GroupTree.Node getSelectedNode() {
		TreePath path = tree.getSelectionPath();
		
		if( path == null)
			return null;
		
		Object obj = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
		if( !(obj instanceof GroupTree.Node)) {
			MDebug.handleWarning( MDebug.WarningType.STRUCTURAL, this, "Selected Node is not a GroupTree.Node");
			return null;
		}
		
		
		return (GroupTree.Node)obj;
	}

    // :::: Paint
    @Override
    public void paint( Graphics g) {
    	super.paint(g);
    }
    
    // :::: MImageStructureObserver interface
	@Override
	public void structureChanged() {
		tree.constructFromWorkspace();
		
	}
    
    private class LayerTree extends JTree 
    	implements TreeCellRenderer, TreeSelectionListener
    {
    	DefaultMutableTreeNode root;
    	DefaultTreeModel model;
    	LayerTreeNodePanel render_panel;
    	
    	Color bgColor;
    	
    	public LayerTree() {
    		render_panel = new LayerTreeNodePanel();
    		
    		// Link Components
    		this.setCellRenderer( this);
    		this.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
    		this.addTreeSelectionListener(this);
    		
    		// Create Model
    		root = new DefaultMutableTreeNode("root");
    		model = new DefaultTreeModel(root);
    		this.setModel( model);
    		
    		
    		// Make the background invisible as we will draw the background manually
    		bgColor = this.getBackground();
    		this.setBackground( new Color(0,0,0,0));

    	}
    	
    	
    	/***
    	 * Called any time the structure of the image has changed, completely removes
    	 * the existing tree structure and recreates it from the GroupTree data.
    	 */
    	private void constructFromWorkspace() {
    		root.removeAllChildren();
    		
    		GroupTree.Node node = master.getCurrentWorkspace().getRootNode();

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
    	public void paint( Graphics g) {
    		
    		// Draw the Background manually so we can draw behind the Tree
    		g.setColor( bgColor);
    		g.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1);

    		// Draw a Background around the 
    		int r = this.getRowForPath( this.getSelectionPath());
    		Rectangle rect = this.getRowBounds(r);
    		
    		if( rect != null) {
    			g.setColor( Color.GRAY);
    			g.fillRect( 0, rect.y, this.getWidth()-1, rect.height-1);
    		}
    		
    		// Let Swing do the heavy lifting
    		super.paint(g);
    		
    	}
    	
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {
				
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
				
				master.getCurrentWorkspace().setActivePart(rn.getRig());
			}
			else
				master.getCurrentWorkspace().setActivePart(null);
			
		}
		
    }

}


