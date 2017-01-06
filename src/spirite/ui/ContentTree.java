package spirite.ui;

import java.awt.Color;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import spirite.Globals;
import spirite.MDebug;

public class ContentTree extends JPanel
	implements MouseListener, TreeSelectionListener, TreeModelListener, TreeExpansionListener
	
{
	private static final long serialVersionUID = 1L;
	protected DefaultMutableTreeNode root;
	protected DefaultTreeModel model;
	protected CCTDragManager dragManager;
	protected JScrollPane scrollPane;
	protected CCPanel container;
	
	protected CCTree tree;
	protected Color bgColor;
	
	protected CCBPanel buttonPanel;

	// :::: Methods triggered by internal events that should can and should be 
	//	overwriten
	protected void moveAbove( TreePath nodeMove, TreePath nodeInto) {}
	protected void moveBelow( TreePath nodeMove, TreePath nodeInto) {}
	protected void moveInto( TreePath nodeMove, TreePath nodeInto, boolean top) {}	
	protected void clickPath( TreePath path, int clickCount) {
		tree.setSelectionPath(path);
	}
	protected void buttonPressed( CCButton button) {}
	protected void buttonCreated( CCButton button) {}

	public ContentTree() {
		// Simple grid layout, fills the whole area
		this.setLayout( new GridLayout());
		container = new CCPanel();
		this.add(container);
		
		buttonPanel = new CCBPanel();
		tree = new CCTree();
		scrollPane = new JScrollPane(container);
		this.add(scrollPane);
		
		// Link Listener
		container.addMouseListener(this);
		tree.addMouseListener(this);
		buttonPanel.addMouseListener(this);
		tree.addTreeSelectionListener(this);	// TODO: this isn't working for some reason
		
		// Single root is invisible, but path is visible
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		
		// Create Model
		root = new DefaultMutableTreeNode("root");
		model = new DefaultTreeModel(root);
		tree.setModel(model);
		
		// Make the background invisible as we will draw the background manually
		bgColor = tree.getBackground();
		container.setOpaque( false);
		tree.setOpaque(false);
		buttonPanel.setOpaque(false);

		// Initialize Drag-Drop Manager
		dragManager = new CCTDragManager();

		dragManager.dragSource = DragSource.getDefaultDragSource();
		dragManager.dgr = 
				dragManager.dragSource.createDefaultDragGestureRecognizer( 
						tree, 
						DnDConstants.ACTION_COPY_OR_MOVE, 
						dragManager);
		
		dragManager.dropTarget = new DropTarget(tree,dragManager);
		
		
		initLayout();
		buttonPanel.reformPanel();
		tree.getModel().addTreeModelListener(this);
	}
	
	/***
	 * Initializes the Layout and the buttons
	 */
	private void initLayout() {
		Dimension size = Globals.getMetric("contentTree.buttonSize", new Dimension(30,30));
		Dimension margin = Globals.getMetric("contentTree.buttonMargin", new Dimension(5,5));
		int bpwidth = size.width + 2*margin.width;

		GroupLayout layout = new GroupLayout( container);
		
		
		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addComponent(buttonPanel, bpwidth, bpwidth, bpwidth)
				.addComponent(tree)
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addComponent(buttonPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
				.addComponent(tree, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
		);
		container.setLayout(layout);
	}
	
	/***
	 * 
	 */
	protected class CCPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		@Override
		public void paint( Graphics g) {
			g.setColor( bgColor);
			g.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1);
			
	
			// Draw a Background around the Selected Path
			int r = tree.getRowForPath( tree.getSelectionPath());
			Rectangle rect = tree.getRowBounds(r);
			
			if( rect != null) {
				if( dragManager.dragIntoNode != null)
					g.setColor( Globals.getColor("contentTree.selectedBGDragging"));
				else
					g.setColor( Globals.getColor("contentTree.selectedBackground"));
				g.fillRect( 0, rect.y, this.getWidth()-1, rect.height-1);
			}
			
			// Draw a Line/Border indicating where you're dragging and dropping
			if( dragManager.dragIntoNode != null) {
				g.setColor( Color.BLACK);
				
				rect = tree.getPathBounds(dragManager.dragIntoNode);
				
				
				switch( dragManager.dragMode) {
				case PLACE_OVER:
					g.drawLine( 0, rect.y, getWidth(), rect.y);
					break;
				case PLACE_UNDER:
					g.drawLine( 0, rect.y+rect.height-1, getWidth(), rect.y+rect.height-1);
					break;
				case PLACE_INTO:
					g.drawRect( 0, rect.y, getWidth()-1, rect.height-1);
					break;
				default:
					break;
				}
			}
			
//			System.out.println(tree.get());
			
			super.paint(g);
		}
	}
	
	
	public class CCBPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		CCButton[] buttons = new CCButton[0];
		
		CCBPanel() {}
		
		public void reformPanel() {
			// Note: margin height is ignored as the gap is calculated from the Size
			Dimension size = Globals.getMetric("contentTree.buttonSize", new Dimension(30,30));
			Dimension margin = Globals.getMetric("contentTree.buttonMargin", new Dimension(5,5));
			
			// Delete the Old Buttons
			for( CCButton button : buttons) {
				remove(button);
			}
			
			// Initialize space for New buttons
			int c = tree.getRowCount();
			buttons = new CCButton[c];
			
			// Construct the skeleton of the layout
			GroupLayout layout = new GroupLayout(this);
					
			GroupLayout.Group hGroup = layout.createParallelGroup();
			GroupLayout.Group vGroup = layout.createSequentialGroup();

			layout.setHorizontalGroup(
					layout.createSequentialGroup()
					.addGap(margin.width)
					.addGroup( hGroup)
				);
			layout.setVerticalGroup(vGroup);
			
			// Add each button and set the layout for them as needed
			int old_y = 0;
			for( int i = 0; i < c; ++i) {
				TreePath path = tree.getPathForRow(i);
				
				Rectangle r = tree.getPathBounds(path);
				int vmargin = Math.max(0,(r.height - size.height) / 2);
				
				buttons[i] = new CCButton(path);
				buttons[i].addActionListener( new ActionListener() {					
					@Override
					public void actionPerformed(ActionEvent e) {
						buttonPressed((CCButton)e.getSource());
					}
				});
				buttonCreated(buttons[i]);
				add( buttons[i]);
				
				hGroup.addComponent( buttons[i], size.width, size.width, size.width);
				
				vGroup.addGap(r.y - old_y + vmargin)
					.addComponent( buttons[i], size.height, size.height, size.height);
				
				old_y = r.y + vmargin + size.height;
			}
			

			
			this.setLayout(layout);
		}
		
		@Override
		public void paint( Graphics g) {
			// Draw the Background manually so we can draw behind the Tree
			super.paint(g);
		}
		
	}
	
	public class CCButton extends JToggleButton {
		private static final long serialVersionUID = 1L;
		int rowNum;
		private TreePath path;
		public CCButton( TreePath path) {
			this.path = path;
		}
		
		public TreePath getAssosciatedTreePath() {
			return path;
		}
	}

	/***
	 * The Tree part of the Tree Panel is separated so that we can add other
	 * components to the DDTree
	 */
	public class CCTree extends JTree {
		private static final long serialVersionUID = 1L;
		public CCTree() {}
		
		@Override
		public void paint( Graphics g) {
			super.paint(g);
		}
	}

	


	
	public static enum DragMode {
		NOT_DRAGGING,
		PLACE_OVER,
		PLACE_UNDER,
		PLACE_INTO,
		HOVER_SELF
	};
	
	/***
	 * The DragManager manages the drag and drop functionality of the tree.
	 *
	 */
	protected class CCTDragManager 
		implements DragGestureListener, DropTargetListener, DragSourceListener
	{
		DragSource dragSource;
		DragGestureRecognizer dgr;
		DropTarget dropTarget;
		
		protected DragMode dragMode = DragMode.NOT_DRAGGING;	
		protected TreePath dragIntoNode = null;
		protected TreePath draggingNode = null;
		
		public CCTDragManager() {
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
			TreePath dragNode = tree.getSelectionPath();
			
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
			// Make sure it's the correct Flavor Type
			DataFlavor dfs[] = evt.getCurrentDataFlavors();
			
			// !!! TODO Figure out how to make the ContentTree DnD not interfere
			//	with the OmniFrame DnD while overlapping areas
			if( dfs.length != 1 || dfs[0] != FLAVOR) {
				if( dfs[0] != OmniFrame.FLAVOR)
//					evt.acceptDrag(DnDConstants.M);
					evt.rejectDrag();
				return;
			}
			
			
			// Then Test based on Position (while updating the data
			//	keeping track of where the tree node will be placed).
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
			TreePath path = getPathFromY(p.y);
			
			if( path != null) {
				try {
					DefaultMutableTreeNode testNode = (DefaultMutableTreeNode)path.getLastPathComponent();
							
				
					Dimension d = Globals.getMetric("contentTree.dragdropLeniency");
					Rectangle r = tree.getPathBounds(path);
					int offset = p.y - r.y;
					
					
					if( testNode == draggingNode.getLastPathComponent())
						changeDrag( path, DragMode.HOVER_SELF);
					else if( testNode.getAllowsChildren() &&
							offset > d.height && offset < r.height - d.height) 
					{
							changeDrag( path, DragMode.PLACE_INTO);
					}
					else if( offset < r.height/2)
						changeDrag( path, DragMode.PLACE_OVER);
					else
						changeDrag( path, DragMode.PLACE_UNDER);
					
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
		
		protected void changeDrag( TreePath newDragInto, DragMode newDragMode) {
			if( newDragInto == dragIntoNode && newDragMode == dragMode)
				return;
			
			dragIntoNode = newDragInto;
			dragMode = newDragMode;
			repaint();
		}

		public void stopDragging() {
			changeDrag( null, DragMode.NOT_DRAGGING);
		}
	
	
		// :::: DragSourceListener
		@Override 		
		public void dragDropEnd(DragSourceDropEvent arg0) {
			// This kind of stuff is SUPPOSED to go in the DragTargetListener drop event
			//	but just to make absolutely sure that there are no async issues with changing
			//	the drag settings to null and since I don't actually use the Transferable
			//	data, I put it here.
			if( dragIntoNode != null) {
				
				if( dragMode == DragMode.PLACE_OVER)
					moveAbove(draggingNode, dragIntoNode);
				else if( ((DefaultMutableTreeNode)dragIntoNode.getLastPathComponent()).getAllowsChildren()) {
					if( dragMode == DragMode.PLACE_UNDER) {
						if( tree.isExpanded(dragIntoNode))
							moveInto( draggingNode, dragIntoNode, true);
						else
							moveBelow( draggingNode, dragIntoNode);
					}
					else if( dragMode == DragMode.PLACE_INTO)
						moveInto( draggingNode, dragIntoNode, false);
				}
				else if( dragMode == DragMode.PLACE_UNDER || dragMode == DragMode.PLACE_INTO) 
					moveBelow(draggingNode, dragIntoNode);
			}
			
			changeDrag( null, DragMode.NOT_DRAGGING);
		}
		@Override		public void dragEnter(DragSourceDragEvent arg0) {}
		@Override		public void dragExit(DragSourceEvent arg0) {}
		@Override		public void dragOver(DragSourceDragEvent evt) {}
		@Override		public void dropActionChanged(DragSourceDragEvent arg0) {}
	}
	
	public TreePath getPathFromY( int y) {
		int c = tree.getRowCount();
		
		for( int i = 0; i < c; ++i) {
			Rectangle r = tree.getRowBounds(i);
			
			if( r.y <= y && r.y + r.height >= y) {
				return tree.getPathForRow(i);
			}
		}
		return null;
	}

	
	// :::: MouseListener
	@Override
	public void mouseClicked(MouseEvent evt) {
		TreePath path = getPathFromY(evt.getY());
		
		if( path != null) {
			clickPath(path, evt.getClickCount());
		}
	}

	@Override	public void mouseEntered(MouseEvent arg0) {}
	@Override	public void mouseExited(MouseEvent arg0) {}
	@Override	public void mousePressed(MouseEvent arg0) {	}
	@Override	public void mouseReleased(MouseEvent arg0) {}

	// :::: TreeSelectionListener
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		// This is needed because the selection UI of the linked ButtonPanel 
		//	has graphics related to the tree's slection that needs to be redrawn
		repaint();
	}

	// :::: TreeModelListener
	@Override	public void treeNodesChanged(TreeModelEvent e) {}

	@Override
	public void treeNodesInserted(TreeModelEvent e) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				buttonPanel.reformPanel();
			}
		});
		
	}

	@Override
	public void treeNodesRemoved(TreeModelEvent e) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				buttonPanel.reformPanel();
			}
		});
		
	}

	@Override
	public void treeStructureChanged(TreeModelEvent e) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				buttonPanel.reformPanel();
			}
		});
	}

	// :::: TreeExpansionListener
	@Override
	public void treeCollapsed(TreeExpansionEvent event) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				buttonPanel.reformPanel();
			}
		});
	}

	@Override
	public void treeExpanded(TreeExpansionEvent event) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				buttonPanel.reformPanel();
			}
		});
	}
}
