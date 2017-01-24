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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import spirite.Globals;
import spirite.MDebug;
import spirite.ui.ContentTree.DragMode;

public class ContentTree extends JPanel
	implements MouseListener, TreeModelListener,TreeExpansionListener, TreeSelectionListener 
{
		
	private static final long serialVersionUID = 1L;
	protected final DefaultMutableTreeNode root;
	protected final DefaultTreeModel model;
	protected final CCTDragManager dragManager;
	protected final JScrollPane scrollPane;
	protected final CCPanel container;
	
	protected final JTree tree;
	protected final Color bgColor;
	
	protected final CCBPanel buttonPanel;

	// Determines the background color to draw the selected node
	// can be re-written by children
	protected Color selectedBG;
	protected Color selectedBGDragging;
	
	private int buttonsPerRow = 0;

	// :::: Methods triggered by internal events that can and should be 
	//	overwritten
	protected void clickPath( TreePath path, MouseEvent clickEvent) {
		tree.setSelectionPath(path);
	}
	protected void buttonPressed( CCButton button) {}
	protected void buttonCreated( CCButton button) {}
	protected boolean allowsHoverOut() { return false;}
	protected Transferable buildTransferable(DefaultMutableTreeNode node) {return null;}
	protected boolean validTransferable(DataFlavor dfs[]) {return false;}
	protected boolean importInto( Transferable trans, TreePath draggingInto, DragMode dragMode) { return false;}

	public ContentTree() {
		// Simple grid layout, fills the whole area
		this.setLayout( new GridLayout());
		container = new CCPanel();
		this.add(container);
		
		buttonPanel = new CCBPanel();
		tree = new JTree();
		scrollPane = new JScrollPane(container);
		this.add(scrollPane);
		
		selectedBG = Globals.getColor("contentTree.selectedBackground");
		selectedBGDragging = Globals.getColor("contentTree.selectedBGDragging");
		
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
		tree.setTransferHandler(dragManager);	// Prevent default tree copy,paste,and drag/drop
		tree.setSelectionModel( new LockingSelectionModel());
		
		initLayout();
		buttonPanel.reformPanel();
		tree.getModel().addTreeModelListener(this);
		

		// Pretty Ugly, but I don't want the tree to change its horizontal scroll
		//	when it starts editing (because it will usually auto-align to hide 
		//	the buttons), so I replace the default F2 action with one that
		//	remembers the Horizontal scrollbar's position and resets it after
		//	editing begins.
		InputMap m = tree.getInputMap( JComponent.WHEN_FOCUSED);
		m.clear();
		m.put( KeyStroke.getKeyStroke( KeyEvent.VK_F2, 0), "myAction");
		tree.getActionMap().put("myAction", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar hsb = scrollPane.getHorizontalScrollBar();
				int value = (hsb == null) ? 0 : hsb.getValue();
				
				tree.startEditingAtPath(tree.getSelectionPath());
				
				hsb = scrollPane.getHorizontalScrollBar();
				if( hsb != null)
					hsb.setValue(value);
			}
		});
		
		// Link Listener
		container.addMouseListener(this);
		tree.addMouseListener(this);
		buttonPanel.addMouseListener(this);
		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
	}
	
	/** Initializes the Outer Layout */
	private void initLayout() {
		//!!!! It's possible that this and CCButtonPanel's reformPanel should be part 
		// of the same method instead of separated.  There's little reason for the layouts
		// to be logically separated.
		Dimension size = Globals.getMetric("contentTree.buttonSize", new Dimension(30,30));
		Dimension margin = Globals.getMetric("contentTree.buttonMargin", new Dimension(5,5));
		int bpwidth = buttonsPerRow*size.width + (buttonsPerRow+1)*margin.width;

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
	
	// ::::  API
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
	
	public void setButtonsPerRow( int num) {
		if( num < 0 ) return;
		
		buttonsPerRow = num;
		initLayout();
		buttonPanel.reformPanel();
	}
	
	/** 
	 * Internal painComponent method which adds more parameters for child
	 * 	classes to mess with
	 * 
	 *   @param drawBG whether or not to draw the base Background Color (so
	 *   child components can draw behind it)
	 */
	protected void paintCCPanel( Graphics g, int width, int height, boolean drawBG) {
		if( drawBG) {
			g.setColor( bgColor);
			g.fillRect( 0, 0, width-1, height-1);
		}
		

		// Draw a Background around the Selected Path
		int r = tree.getRowForPath( tree.getSelectionPath());
		Rectangle rect = tree.getRowBounds(r);
		
		if( rect != null) {
			if( dragManager.dragIntoNode != null)
				g.setColor( selectedBGDragging);
			else
				g.setColor( selectedBG);
			g.fillRect( 0, rect.y, width-1, rect.height-1);
		}
		
		// Draw a Line/Border indicating where you're dragging and dropping
		if( dragManager.dragIntoNode != null) {
			g.setColor( Color.BLACK);
			
			rect = tree.getPathBounds(dragManager.dragIntoNode);
			
			
			switch( dragManager.dragMode) {
			case PLACE_OVER:
				g.drawLine( 0, rect.y, width, rect.y);
				break;
			case PLACE_UNDER:
				g.drawLine( 0, rect.y+rect.height-1, width, rect.y+rect.height-1);
				break;
			case PLACE_INTO:
				g.drawRect( 0, rect.y, width-1, rect.height-1);
				break;
			default:
				break;
			}
		}
		else if( dragManager.dragMode == DragMode.HOVER_OUT) {
			Rectangle rect2 = tree.getRowBounds(tree.getRowCount()-1);
			
			
			g.drawLine( 0, rect2.y+rect2.height+2, width,  rect2.y+rect2.height+2);
		}
		
	}
	protected class CCPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		@Override
		public void paintComponent( Graphics g) {
			paintCCPanel( g, getWidth(), getHeight(), true);

			super.paintComponent(g);
		}
	}
	
	/** The panel to the left of the tree that constructs the buttons parallel
	 * to the tree nodes. 
	 */
	public class CCBPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private CCButton[][] buttons = new CCButton[0][];
		
		CCBPanel() {}
		
		public void reformPanel() {
			// Note: margin height is ignored as the gap is calculated from the Size
			Dimension size = Globals.getMetric("contentTree.buttonSize", new Dimension(30,30));
			Dimension margin = Globals.getMetric("contentTree.buttonMargin", new Dimension(5,5));
			
			// Delete the Old Buttons
			for( CCButton[] row : buttons) {
				for( CCButton button : row)
					remove(button);
			}
			
			
			// Construct the skeleton of the layout
			GroupLayout layout = new GroupLayout(this);
					
			GroupLayout.Group hGroup = layout.createParallelGroup();
			GroupLayout.Group vGroup = layout.createSequentialGroup();
			
			layout.setHorizontalGroup(hGroup);
			layout.setVerticalGroup(vGroup);
			
			if( buttonsPerRow != 0) {
				// Initialize space for New buttons
				int c = tree.getRowCount();
				buttons = new CCButton[ c][];
				
				
				// Add each button and set the dynamic layout for them as needed
				int old_y = 0;
				for( int i = 0; i < c; ++i) {
					buttons[i] = new CCButton[buttonsPerRow];
					TreePath path = tree.getPathForRow(i);
					
					Rectangle r = tree.getPathBounds(path);
					int vmargin = Math.max(0,(r.height - size.height) / 2);
					
					GroupLayout.Group innerHGroup = layout.createSequentialGroup();
					GroupLayout.Group innerVGroup = layout.createParallelGroup();
					
					for( int j = 0; j < buttonsPerRow; ++j) {
						buttons[i][j] = new CCButton(path, j);
						buttons[i][j].addActionListener( new ActionListener() {					
							@Override
							public void actionPerformed(ActionEvent e) {
								buttonPressed((CCButton)e.getSource());
							}
						});
						buttonCreated(buttons[i][j]);
						
						innerHGroup.addGap(margin.width)
								.addComponent( buttons[i][j], size.width, size.width, size.width);
						innerVGroup.addComponent(buttons[i][j], size.height, size.height, size.height);
					}
					
					innerHGroup.addGap(margin.width);
					
					hGroup.addGroup(innerHGroup);
					vGroup.addGap(r.y - old_y + vmargin)
						.addGroup(innerVGroup);
					
					old_y = r.y + vmargin + size.height;
				}
			}
			
			
			this.setLayout(layout);
		}
		
		@Override
		public void paintComponent( Graphics g) {
			// Draw the Background manually so we can draw behind the Tree
			super.paintComponent(g);
		}
		
		public int getButtonRowCount() {
			return buttons.length;
		}
		public CCButton getButtonAt( int i, int j) {
			if( i < 0 || j < 0) return null;
			if( i >= buttons.length)
				return null;
			if( j >= buttons[i].length)
				return null;
			return buttons[i][j];
		}
		
	}
	
	/** A Button that is linked to a TreePath*/
	public class CCButton extends JToggleButton {
		private static final long serialVersionUID = 1L;
		public final int buttonNum;
		private final TreePath path;
		public CCButton( TreePath path, int num) {
			this.path = path;
			this.buttonNum = num;
		}
		
		public TreePath getAssosciatedTreePath() {
			return new TreePath(path.getPath());
		}
	}

	

	
	public static enum DragMode {
		NOT_DRAGGING,
		PLACE_OVER,
		PLACE_UNDER,
		PLACE_INTO,
		HOVER_SELF,
		HOVER_OUT
	};
	

	// Kind of ugly, but this is the easiest way I can determine to prevent the 
	//	Tree from changing its selection while dragging nodes.
	private static boolean locked = false;
	private class LockingSelectionModel extends DefaultTreeSelectionModel {
		@Override public void addSelectionPath(TreePath path) {
			if( !locked)
				super.addSelectionPath(path);
		}
		@Override public void removeSelectionPath(TreePath path) {
			if( !locked)
				super.removeSelectionPath(path);
		}
		@Override
		public void setSelectionPath(TreePath path) {
			if( !locked)
				super.setSelectionPath(path);
		}
		@Override
		public void clearSelection() {
			if( !locked)
				super.clearSelection();
		}
	}

	
	/** The DragManager manages the drag and drop functionality of the tree.*/
	protected class CCTDragManager extends TransferHandler
		implements DragGestureListener, DragSourceListener
	{
		private final DragSource dragSource;
		
		protected DragMode dragMode = DragMode.NOT_DRAGGING;	
		protected TreePath dragIntoNode = null;
		protected TreePath draggingNode = null;
		
		public CCTDragManager() {
			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer( 
							tree, 
							DnDConstants.ACTION_COPY_OR_MOVE, 
							this);
		}
		
	
		// :::: Import
		@Override
		public boolean canImport(TransferSupport support) {
			// Make sure it's the correct Flavor Type
			DataFlavor dfs[] = support.getDataFlavors();
			
			// !!! TODO Figure out how to make the ContentTree DnD not interfere
			//	with the OmniFrame DnD while overlapping areas
			if( !validTransferable(dfs)) {
				if( dfs.length >= 1 && dfs[0] != OmniFrame.FLAVOR){
					
				}
				return false;
			}
			
			
			// Then Test based on Position (while updating the data
			//	keeping track of where the tree node will be placed).
			if(!testDrag( support.getDropLocation().getDropPoint())) {
				return false;
			}
			else {
				return true;
			}
		}
		
		@Override
		public boolean importData(TransferSupport support) {
			locked = false;
			
			if( validTransferable(support.getDataFlavors())) {
				return importInto(support.getTransferable(), dragIntoNode, dragMode);
			}
			
			return false;
		}
		
		// :::: DragGesterRecignizer
		@Override
		public void dragGestureRecognized(DragGestureEvent evt) {
			TreePath dragNode = tree.getSelectionPath();
			
			if( dragNode != null) {
				draggingNode = dragNode;
				

				Transferable trans = buildTransferable((DefaultMutableTreeNode)(dragNode.getLastPathComponent()));
				
				if( trans == null)
					trans = new NilTransferable();
				
				// Set the cursor and start the drag action
				Cursor cursor = DragSource.DefaultMoveDrop;
				int action = evt.getDragAction();
				if( action == DnDConstants.ACTION_MOVE)
					cursor = DragSource.DefaultMoveDrop;
				
				locked = true;
				dragSource.startDrag( evt, cursor, trans, this);
			}
		}
		private class NilTransferable implements Transferable {
			NilTransferable( ) {}
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if( flavor.equals(FLAVOR))	return this;
				else throw new UnsupportedFlavorException(flavor);
			}
			@Override public DataFlavor[] getTransferDataFlavors() {return flavors;}
			@Override public boolean isDataFlavorSupported(DataFlavor flavor) {return flavor.equals(FLAVOR);}
		}
		final public DataFlavor FLAVOR = new DataFlavor( NilTransferable.class, "Nil Transferable (Unimplemented)");
		private final DataFlavor flavors[] = {FLAVOR};
		
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

			System.out.println(p);
			
			if( path != null) {
				try {
					DefaultMutableTreeNode testNode = (DefaultMutableTreeNode)path.getLastPathComponent();
							
				
					Dimension d = Globals.getMetric("contentTree.dragdropLeniency");
					Rectangle r = tree.getPathBounds(path);
					int offset = p.y - r.y;
					
					
					if( draggingNode != null &&testNode == draggingNode.getLastPathComponent())
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
			else if( allowsHoverOut()) {
				changeDrag(path, DragMode.HOVER_OUT);
				return true;
			}
			
			
			
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
			locked = false;
			changeDrag( null, DragMode.NOT_DRAGGING);
			draggingNode = null;
		}
		@Override		public void dragEnter(DragSourceDragEvent arg0) {}
		@Override		public void dragExit(DragSourceEvent arg0) {}
		@Override		public void dragOver(DragSourceDragEvent evt) {}
		@Override		public void dropActionChanged(DragSourceDragEvent arg0) {}
	}

	
	// :::: MouseListener
	@Override	public void mouseEntered(MouseEvent arg0) {}
	@Override	public void mouseExited(MouseEvent arg0) {}
	@Override	public void mousePressed(MouseEvent arg0) {	}
	@Override	public void mouseReleased(MouseEvent arg0) {}
	@Override
	public void mouseClicked(MouseEvent evt) {
		TreePath path = getPathFromY(evt.getY());
		
		if( path != null) {
			clickPath(path, evt);
		}
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
	

	// :::: TreeSelectionListener
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		// This is needed because the selection UI of the linked ButtonPanel 
		//	has graphics related to the tree's slection that needs to be redrawn
		repaint();
	}
	
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
