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
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageStructureObserver;

public class LayerTreePanel extends JPanel 
	implements MImageStructureObserver
{
	MasterControl master;
	LayerTree tree;
	ImageWorkspace workspace;

	// :::: Initialize
	public LayerTreePanel( MasterControl master) {
		this.master = master;
		workspace = master.getCurrentWorkspace();
		initComponents();
		
		workspace.addImageStructureObserver(this);
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
		return tree.getSelectedNode();
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
    	implements TreeCellRenderer, TreeSelectionListener, TreeExpansionListener
    {
    	DefaultMutableTreeNode root;
    	DefaultTreeModel model;
    	LayerTreeNodePanel renderPanel;
    	LTPDragManager dragManager;
    	
    	Color bgColor;
    	
    	public LayerTree() {
    		renderPanel = new LayerTreeNodePanel();
    		
    		// Link Components
    		this.setCellRenderer( this);
    		this.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
    		this.addTreeSelectionListener(this);
    		this.addTreeExpansionListener(this);
    		
    		// Create Model
    		root = new DefaultMutableTreeNode("root");
    		model = new DefaultTreeModel(root);
    		this.setModel( model);
    		
    		
    		// Make the background invisible as we will draw the background manually
    		bgColor = this.getBackground();
    		this.setBackground( new Color(0,0,0,0));
    		
    		
    		// Initialize Drag-Drop Manager
    		dragManager = new LTPDragManager();

    		dragManager.dragSource = DragSource.getDefaultDragSource();
    		dragManager.dgr = 
    				dragManager.dragSource.createDefaultDragGestureRecognizer( 
    						this, 
    						DnDConstants.ACTION_COPY_OR_MOVE, 
    						dragManager);
    		
    		dragManager.dropTarget = new DropTarget(this,dragManager);
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
    		tree.repaint();
    		
    		dragManager.changeDrag(null, 0);
    	}
    	private void _cfw_construcRecursively( GroupTree.Node group_node, DefaultMutableTreeNode tree_node) {
    		for( GroupTree.Node child : group_node.getChildren()) {
    			DefaultMutableTreeNode node_to_add = new DefaultMutableTreeNode(child);
    			
    			tree_node.add( node_to_add);
    			
    			_cfw_construcRecursively( child, node_to_add);
    		}
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
    		} catch( ClassCastException e) {}
    	}

    	
    	/***
    	 * 
    	 */
    	@Override
    	public void paint( Graphics g) {
    		Component parent = this.getParent();
    		
    		// Draw the Background manually so we can draw behind the Tree
    		g.setColor( bgColor);
    		g.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1);

    		// Draw a Background around the Selected Path
    		int r = this.getRowForPath( this.getSelectionPath());
    		Rectangle rect = this.getRowBounds(r);
    		
    		if( rect != null) {
    			if( dragManager.dragIntoNode != null)
   					g.setColor( Globals.getColor("layerpanel.tree.selectedBGDragging"));
    			else
    				g.setColor( Globals.getColor("layerpanel.tree.selectedBackground"));
    			g.fillRect( 0, rect.y, this.getWidth()-1, rect.height-1);
    		}
    		
    		// Draw a Line/Border indicating where you're dragging and dropping
    		if( dragManager.dragIntoNode != null) {
    			g.setColor( Color.BLACK);
    			
    			rect = this.getPathBounds(dragManager.dragIntoNode);
    			
    			switch( dragManager.dragMode) {
    			case 1:
    				g.drawLine( 0, rect.y, parent.getWidth(), rect.y);
    				break;
    			case 2:
    				g.drawLine( 0, rect.y+rect.height-1, parent.getWidth(), rect.y+rect.height-1);
    				break;
    			case 3:
    				g.drawRect( 0, rect.y, parent.getWidth()-1, rect.height-1);
    				break;
    			}
    		}
    		
    		// Let Swing do the heavy lifting
    		super.paint(g);
    		
    		
    	}
    	
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {
				
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
			if( obj instanceof GroupTree.RigNode) {
				GroupTree.RigNode rn = (GroupTree.RigNode)obj;
				

				renderPanel.label.setText(rn.getRig().getName());
			}
			return renderPanel;
		}
		

		/***
		 * Called whenever the user has selected a new tree node, updates the Workspace so that the 
		 * active part (the part that gets drawn on) is changed
		 */
		@Override
		public void valueChanged(TreeSelectionEvent tse) {			
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
			
			Object obj = node.getUserObject();
			
			if( obj instanceof GroupTree.RigNode) {
				GroupTree.RigNode rn = (GroupTree.RigNode)obj;
				
				workspace.setActivePart(rn.getRig());
			}
			else
				workspace.setActivePart(null);
			
		}

		// :::: TreeExpansionListener
		@Override
		public void treeCollapsed(TreeExpansionEvent evt) {
			try {
				GroupTree.Node node = 
						(GroupTree.Node)((DefaultMutableTreeNode)evt.getPath().getLastPathComponent()).getUserObject();
				
				node.setExpanded(false);
			} catch ( ClassCastException e) {
				MDebug.handleWarning(MDebug.WarningType.STRUCTURAL, this, "Bad Tree Class Type");
			}
		}

		@Override
		public void treeExpanded(TreeExpansionEvent evt) {
			try {
				GroupTree.Node node = 
						(GroupTree.Node)((DefaultMutableTreeNode)evt.getPath().getLastPathComponent()).getUserObject();
				
				node.setExpanded(true);
			} catch ( ClassCastException e) {
				MDebug.handleWarning(MDebug.WarningType.STRUCTURAL, this, "Bad Tree Class Type");
			}
			
		}
    }
    
    /*** 
     * <b>LTPDragManager</b>
     * 
     * Drag-and-Drop management, handled in a separate class for aesthetic reasons
     */
    private class LTPDragManager 
    	implements DragGestureListener, DropTargetListener, DragSourceListener
    {
    	DragSource dragSource;
    	DragGestureRecognizer dgr;
    	DropTarget dropTarget;
		
    	// 0: None, 1: Over, 2: Under, 3: Into, 4: Hovering above itself
		protected int dragMode = 0;	
		protected TreePath dragIntoNode = null;
		protected GroupTree.Node draggingNode = null;
    	
    	public LTPDragManager() {
		}
    	

		/***
		 * Simple Transferable object that only acts as an identifier
		 */
		private class ContainerNode implements Transferable {
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if( flavor.equals(FLAVOR))
					return this;
				else throw new UnsupportedFlavorException(flavor);
			}

			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return flavors;
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return flavor.equals(FLAVOR);
			}
			
		}
		final public DataFlavor FLAVOR = 
				new DataFlavor( ContainerNode.class, "Container Node");
		private DataFlavor flavors[] = {FLAVOR};
		
		// :::: DragGesterRecignizer
		@Override
		public void dragGestureRecognized(DragGestureEvent evt) {
			// TODO Auto-generated method stub
			GroupTree.Node dragNode = getSelectedNode();
			
			if( dragNode != null) {
				ContainerNode containerNode = new ContainerNode();
				draggingNode = dragNode;
				Transferable trans = (Transferable) containerNode;
				
				// Set the cursor and start the drag action
				Cursor cursor = DragSource.DefaultMoveDrop;
				int action = evt.getDragAction();
				if( action == DnDConstants.ACTION_MOVE)
					cursor = DragSource.DefaultMoveDrop;
				
				dragSource.startDrag( evt, cursor, trans, this);
			}
		}
		
		// :::: DragTargetListener
		@Override		public void dragEnter(DropTargetDragEvent arg0) {}
		@Override		public void dragExit(DropTargetEvent arg0) {}
		@Override		public void dropActionChanged(DropTargetDragEvent arg0) {}
		@Override		public void drop(DropTargetDropEvent arg0) {}

		@Override
		public void dragOver(DropTargetDragEvent evt) {
			if(!testDrag( evt.getLocation())) {
				evt.rejectDrag();
			}
			else {
				evt.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
			}
			
		}
		
		/***
		 * Tests whether the point is a valid and stores what kind of drop it would be
		 * for visual purposes
		 * 
		 * @param p point to test
		 * @return true if point is valid, false otherwise
		 */
		private boolean testDrag( Point p ) {
			// Doing it like this makes it so only the vertical position is relvant
			TreePath path = tree.getPathForRow(tree.getRowForLocation(p.x, p.y));
			
			if( path != null) {
				try {
					GroupTree.Node testNode = 
							(GroupTree.Node)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
				
					Dimension d = Globals.getMetric("layerpanel.treenodes.dragdropleniency");
					Rectangle r = tree.getPathBounds(path);
					int offset = p.y - r.y;
					
					
					if( testNode == draggingNode)
						changeDrag( path, 4);
					else if( (testNode instanceof GroupTree.GroupNode) &&
							offset > d.height && offset < r.height - d.height) 
					{
							changeDrag( path, 3);
					}
					else if( offset < r.height/2)
						changeDrag( path, 1);
					else
						changeDrag( path, 2);
					
					return true;
					
				}catch( ClassCastException e) {
					MDebug.handleWarning( MDebug.WarningType.STRUCTURAL, this, "Tree node you're dragging isn't correct class.");				
				}catch( NullPointerException e) {
					MDebug.handleWarning( MDebug.WarningType.UNSPECIFIED, this, "NullPointer in testDrag (probably sync issue)");
				}
			}
			
			changeDrag( dragIntoNode, dragMode);
			return false;
		}
		
		protected void changeDrag( TreePath newDragInto, int newDragMode) {
			if( newDragInto == dragIntoNode && newDragMode == dragMode)
				return;
			
			dragIntoNode = newDragInto;
			dragMode = newDragMode;
			tree.getParent().repaint();
		}



		// :::: DragSourceListener
		@Override 		
		public void dragDropEnd(DragSourceDropEvent arg0) {
			// This kind of stuff is SUPPOSED to go in the DragTargetListener drop event
			//	but just to make absolutely sure that there are no async issues with changing
			//	the drag settings to null and since I don't actually use the Transferable
			//	data, I put it here.
			if( dragIntoNode != null) {
				GroupTree.Node nodeInto = 
						(GroupTree.Node)((DefaultMutableTreeNode)dragIntoNode.getLastPathComponent()).getUserObject();
				if( dragMode == 1) 
					workspace.moveAbove(draggingNode, nodeInto);
				else if( nodeInto instanceof GroupTree.GroupNode) {
					if( dragMode == 2) {
						if( tree.isExpanded(dragIntoNode))
							workspace.moveIntoTop(draggingNode, (GroupTree.GroupNode)nodeInto);
						else
							workspace.moveBelow(draggingNode, nodeInto);
					}
					else if( dragMode == 3)
						workspace.moveInto(draggingNode, ( GroupTree.GroupNode)nodeInto);
				}
				else if( dragMode == 2 || dragMode == 3) 
					workspace.moveBelow(draggingNode, nodeInto);
			}
			
			changeDrag( null, 0);
		}
		@Override		public void dragEnter(DragSourceDragEvent arg0) {}
		@Override		public void dragExit(DragSourceEvent arg0) {}
		@Override		public void dragOver(DragSourceDragEvent evt) {}
		@Override		public void dropActionChanged(DragSourceDragEvent arg0) {}
    }

}


