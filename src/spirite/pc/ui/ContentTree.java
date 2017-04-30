package spirite.pc.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
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

import spirite.pc.Globals;
import spirite.pc.MDebug;

public class ContentTree extends JPanel
	implements MouseListener, TreeModelListener,TreeExpansionListener, TreeSelectionListener 
{
		
	private static final long serialVersionUID = 1L;
	protected final DefaultMutableTreeNode root;
	protected final DefaultTreeModel model;
	protected final CCTTransferHandler transferHandler;
	protected final JScrollPane scrollPane;
	protected final CCPanel container;
	protected final LockingSelectionModel selectionModel = new LockingSelectionModel();
	
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
	protected boolean importOut(Transferable trans) {return false;}
	protected boolean importAbove(Transferable trans, TreePath path) {return false;}
	protected boolean importBelow(Transferable trans, TreePath path) {return false;}
	protected boolean importSelf(Transferable trans, TreePath path) {return false;}
	protected boolean importInto(Transferable trans, TreePath path, boolean top) {return false;}
	protected boolean importClear( TreePath path) {return false;}
	
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
		transferHandler = new CCTTransferHandler();
		tree.setTransferHandler(null);
		this.setTransferHandler(transferHandler);	// Prevent default tree copy,paste,and drag/drop
		tree.setSelectionModel(selectionModel);
		
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
		this.addMouseListener(this);
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
	
	protected Color getColor( int row) {
		if( tree.isRowSelected(row)) {
			if( transferHandler.dragMode != DragMode.NOT_DRAGGING)
				return selectedBGDragging;
			return selectedBG;
		}
		return null;
	}
	
	
	/**
	 * Called to prevent excessive, potentially cyclical rebuilds
	 */
	private boolean building = false;
	protected void startBuilding() {
		building = true;
	}
	protected void finishBuilding() {
		building = false;
		
		buttonPanel.reformPanel();
	}
	
	
	protected class CCPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		@Override
		public void paintComponent( Graphics g) {
			super.paintComponent(g);

			if( bgColor != null) {
				g.setColor( bgColor);
				g.fillRect( 0, 0, getWidth()-1, getHeight() -1);
			}
			

			for( int i=0; i<tree.getRowCount(); ++i) {
				Color c = getColor( i);
				
				if( c != null ) {
					Rectangle rect = tree.getRowBounds(i);
					
					if( rect != null) {
						g.setColor(c);
						g.fillRect( 0, rect.y, getWidth(), rect.height);
					}
				}
			}
			// Draw a Line/Border indicating where you're dragging and dropping
			if( transferHandler.dragIntoNode != null) {
				g.setColor( Color.BLACK);
				
				Rectangle rect = tree.getPathBounds(transferHandler.dragIntoNode);
				
				
				switch( transferHandler.dragMode) {
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
			else if( transferHandler.dragMode == DragMode.HOVER_OUT) {
				Rectangle rect2 = tree.getRowBounds(tree.getRowCount()-1);
				int dy = (rect2==null)?3:rect2.y+rect2.height+2;
				
				g.drawLine( 0, dy, getWidth(),  dy);
			}
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
			if( building) return;
			
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
	private class LockingSelectionModel extends DefaultTreeSelectionModel {
		private boolean locked = false;
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
	
	// Even uglier, but hey.
	private static boolean imported = false;

	
	/** The DragManager manages the drag and drop functionality of the tree.*/
	protected class CCTTransferHandler extends TransferHandler
		implements DragGestureListener, DragSourceListener
	{
		private final DragSource dragSource;
		
		protected DragMode dragMode = DragMode.NOT_DRAGGING;	
		protected TreePath dragIntoNode = null;
		protected TreePath draggingNode = null;
		
		public CCTTransferHandler() {
			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer( 
							tree, 
							DnDConstants.ACTION_COPY_OR_MOVE, 
							this);
		}

		// :::: Export 
		// Unused to make sure no default mechanisms get in our way
		@Override public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {}
		@Override public void exportAsDrag(JComponent comp, InputEvent e, int action) {}
		@Override		public int getSourceActions( JComponent c) {return -1;}
		@Override		public Transferable createTransferable( JComponent c) {	return null;}
		@Override 		public void exportDone( JComponent c, Transferable t, int action) {}
		
	
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

			Point p = SwingUtilities.convertPoint( support.getComponent(), 
					support.getDropLocation().getDropPoint(), 
					tree);

			// Then Test based on Position (while updating the data
			//	keeping track of where the tree node will be placed).
			if(!testDrag( p)) {
				return false;
			}
			else {
				return true;
			}
		}
		
		@Override
		public boolean importData(TransferSupport support) {
			selectionModel.locked = false;
			imported = importInner(support);
			
			
			return imported;
		}
		private boolean importInner( TransferSupport support) {
			if( validTransferable(support.getDataFlavors())) {
				Transferable trans = support.getTransferable();

				if( dragMode == DragMode.HOVER_OUT) {
					return importOut(trans);
				}
				else if( dragMode == DragMode.HOVER_SELF)
					return importSelf(trans, dragIntoNode);
				else if( dragIntoNode != null) {					
					if( dragMode == DragMode.PLACE_OVER)
						return importAbove( trans, dragIntoNode);
					else if( ((DefaultMutableTreeNode)dragIntoNode.getLastPathComponent()).getAllowsChildren()){
						if( dragMode == DragMode.PLACE_UNDER) {
							if( tree.isExpanded(dragIntoNode) )
								return importInto( trans, dragIntoNode, true);
							else
								return importBelow( trans, dragIntoNode);
						}
						if( dragMode == DragMode.PLACE_INTO)
							return importInto( trans, dragIntoNode, false);
					}
					else if( dragMode == DragMode.PLACE_UNDER || dragMode == DragMode.PLACE_INTO) 
						return importBelow( trans, dragIntoNode);
				}
			}
			return false;
		}
		
		// :::: DragGesterRecignizer
		@Override
		public void dragGestureRecognized(DragGestureEvent evt) {
			if( evt.getTriggerEvent() instanceof MouseEvent &&
				((MouseEvent)evt.getTriggerEvent()).getButton() != 1)
				return;
			
			TreePath dragNode = getPathFromY( evt.getDragOrigin().y);
			
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
				
				selectionModel.locked = true;
				imported = false;
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
			selectionModel.locked = false;
			if( !imported)
				importClear( draggingNode);
			
			changeDrag( null, DragMode.NOT_DRAGGING);
			draggingNode = null;
			
		}
		@Override		public void dragEnter(DragSourceDragEvent arg0) {}
		@Override		public void dragExit(DragSourceEvent arg0) {}
		@Override		public void dragOver(DragSourceDragEvent evt) {}
		@Override		public void dropActionChanged(DragSourceDragEvent arg0) {}
	}

	
	// :::: MouseListener
	@Override public void mouseEntered(MouseEvent arg0) {}
	@Override public void mouseExited(MouseEvent arg0) {}
	@Override public void mousePressed(MouseEvent arg0) {	}
	@Override public void mouseClicked(MouseEvent evt) {}
	@Override	public void mouseReleased(MouseEvent evt) {
		if(this.transferHandler.dragMode == DragMode.NOT_DRAGGING) {
			TreePath path = getPathFromY(evt.getY());

			clickPath(path, evt);
		}
	}

	// :::: TreeModelListener
	@Override public void treeNodesChanged(TreeModelEvent e) {}
	@Override
	public void treeNodesInserted(TreeModelEvent e) {
				buttonPanel.reformPanel();
	}
	@Override
	public void treeNodesRemoved(TreeModelEvent e) {
			buttonPanel.reformPanel();
	}
	@Override
	public void treeStructureChanged(TreeModelEvent e) {
			buttonPanel.reformPanel();
	}
	
	// :::: TreeSelectionListener
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
			transferHandler.stopDragging();
	}
	
	@Override
	public void treeCollapsed(TreeExpansionEvent event) {
			buttonPanel.reformPanel();
	}

	@Override
	public void treeExpanded(TreeExpansionEvent event) {
			buttonPanel.reformPanel();
	}
}
