package spirite.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import spirite.MDebug.WarningType;
import spirite.MDebug;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.ui.FrameManager.FrameType;
import spirite.ui.OmniFrame.OmniContainer;

public class OmniFrame extends JDialog
{	
	private static final long serialVersionUID = 1L;

	private MasterControl master;
	
	// Components
	private OFTransferHandler transferHandler;
	private JTabbedPane root;
	private List<OmniContainer> containers = new ArrayList<>();
	
	// Drag UI States
	private enum DragMode {
		NOT_DRAGGING,
		DRAG_INTO_TAB,
		DRAG_INTO_CONTENT
	}
	private DragMode dragMode = DragMode.NOT_DRAGGING;
	private int dragTab = 0;
	
	public OmniFrame( MasterControl master, FrameType type) {
		this.master = master;
		initComponents();
		
		// Create the panel of the given type
		addPanel( type);	
		
	}
	
	public OmniFrame(MasterControl master, OmniContainer container) {
		this.master = master;
		initComponents();
		
		addContainer(container, -1);
	}

	private void initComponents() {
		root = new OmniTabbedFrame();
		root.setTabPlacement(JTabbedPane.TOP);
		root.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		//root.addMouseListener(this);
		
		//getGlassPane().setVisible(true);
		//getGlassPane().addMouseListener(this);
		//getGlassPane().addMouseMotionListener(this);;
		
		//
		transferHandler = new OFTransferHandler(this);
		root.setTransferHandler(transferHandler);
		
		// Create TabbedPane
		this.add( root);
		
	}
	
	/***
	 * This classes is needed because overriding a JDialog's paint method
	 * is not nearly as effective as overriding a Component's
	 */
	public class OmniTabbedFrame extends JTabbedPane {
		private static final long serialVersionUID = 1L;

		OmniTabbedFrame() {
			super();
		}

		@Override
		public void paint( Graphics g) {
			super.paint(g);

			switch( dragMode) {
			case DRAG_INTO_TAB:
				Rectangle r = root.getBoundsAt(dragTab);
				
                Graphics2D g2 = (Graphics2D) g;
                Stroke oldStroke = g2.getStroke();
				g2.setColor( Color.BLACK);
				g2.setStroke( new BasicStroke(3));
				g2.drawRect( r.x, r.y, r.width, r.height);
				g2.setStroke( oldStroke);
				break;
			case DRAG_INTO_CONTENT:
				g.drawRect(0, 0, getWidth()-1, getHeight()-1);
				break;
			default:
				break;
			}
			
		}
	}
	
	
	/*** 
	 * Adds Panel of the given FrameType
	 */
	public void addPanel( FrameType type) {
		JPanel panel = master.getFrameManager().createOmniPanel(type);
		
		if( panel == null) return;
		
		root.addTab("tab", panel);
		
		OmniBar bar = new OmniBar( type.getName());
		root.setTabComponentAt(root.getTabCount()-1, bar);
		
		containers.add(new OmniContainer(panel, bar, type));
		transferHandler.refreshGestureRecognizers();
	}
	
	/***
	 * @return All the FrameTypes contained in the OmniFrame
	 */
	public List<FrameType> getContainedFrameTypes() {
		List<FrameType> list = new ArrayList<FrameType>();

		for( OmniContainer container : containers) {
			list.add( container.type);
		}
		
		return list;
	}
	
	/***
	 * @return True if the OmniPanel contains any frame of the given Type
	 */
	public boolean containsFrameType( FrameType type) {
		for( OmniContainer container : containers) {
			if( container.type == type)
				return true;
		}
		
		return false;
	}
	
	// :::: OmniFrame-to-Omniframe Panel Docking methods
	private void removeContainer( OmniContainer toRemove) {
		int index = containers.indexOf( toRemove);
		if( index == -1) {
			MDebug.handleWarning( WarningType.STRUCTURAL, this, "Tried to remove a OmniTab that doesn't exist.");
			return;
		}
		
		root.remove(index);
		containers.remove(index);
		transferHandler.refreshGestureRecognizers();
		
		// If this OmniFrame holds no other tabs, remove it
		if( containers.size() == 0) {
			this.dispose();
		}
	}
	private void addContainer( OmniContainer toAdd, int index) {
		if( index == -1) {
			root.add(toAdd.panel);
			containers.add(toAdd);
			index = containers.size()-1;
		}
		else {
			root.add(toAdd.panel, index);
			containers.add(index,toAdd);
		}
		toAdd.bar = new OmniBar( toAdd.type.getName());
		root.setTabComponentAt(index, toAdd.bar);
		root.setSelectedIndex(index);
		transferHandler.refreshGestureRecognizers();
	}
	
	
	/***
	 * Custom Tab
	 */
	public class OmniBar extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;
		
		JLabel label;
		public OmniBar( String title) {
			label = new JLabel(title);
			add( label);
			this.setOpaque(false);
			addMouseListener(this);
		}

		// Required so that the custom tab component behaves like a tab.
		@Override
		public void mouseClicked(MouseEvent evt) {
			if( evt.getButton() == MouseEvent.BUTTON1) {
				int i, c;
				
				c = root.getTabCount();
				for( i=0; i<c; ++i) {
					if( root.getTabComponentAt(i)== this) {
						root.setSelectedIndex(i);
					}
				}
				
			}
		}
		@Override		public void mouseEntered(MouseEvent e) {}
		@Override		public void mouseExited(MouseEvent e) {}
		@Override		public void mousePressed(MouseEvent e) {}
		@Override		public void mouseReleased(MouseEvent e) {}
	}
	
	static class OmniContainer {
		JPanel panel;
		OmniBar bar;	// It's possible that this shouldn't be here.  It helps
						//  simplify some internal code, but it is tied closely to
						//  the particular OmniFrame that it is currently housed
						//  in whereas the OmniContainer class is more abstract
		FrameType type;
		
		OmniContainer( JPanel panel, OmniBar bar, FrameType type) {
			this.panel = panel;
			this.bar = bar;
			this.type = type;
		}
	}
	
	
	/***
	 * 
	 */
	private static class OFTransferable implements Transferable {
		OmniFrame parent;
		OmniContainer panel;
		
		OFTransferable( OmniFrame parent, OmniContainer container) {
			this.parent = parent;
			this.panel = container;
		}
		
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
	public final static DataFlavor FLAVOR = 
			new DataFlavor( OFTransferable.class, "OmniPanel");
	private static DataFlavor flavors[] = {FLAVOR};
	
	
	/***     
	 * The Transfer Handler handles all of the Drag-and-Drop code relating to
	 *  OmniPanels
	 */
	protected class OFTransferHandler extends TransferHandler 
		implements DragGestureListener, DragSourceListener
	{
		private static final long serialVersionUID = 1L;
		protected DragSource dragSource;
		protected List<DragGestureRecognizer> dgrs = new ArrayList<>();
		protected DropTarget dropTarget;
		private OmniFrame context;
		private OmniContainer dragging = null;	// Used only for dragging a container out of a frame
		
		public OFTransferHandler(OmniFrame context) {
			this.context = context;
			dragSource = DragSource.getDefaultDragSource();
		}
		
		
		/**
		 * Called when the tab structure changed, removes all existing
		 * Gesture Recognizers and then adds new ones for each custom tab
		 * component.
		 */
		protected void refreshGestureRecognizers() {
			for( DragGestureRecognizer dgr : dgrs) {
				dgr.setComponent(null);
			}
			dgrs.clear();
			
			for( OmniContainer container : containers) {
				dgrs.add(
					dragSource.createDefaultDragGestureRecognizer(
						container.bar,
						DnDConstants.ACTION_COPY_OR_MOVE,
						this));
			}
		}
		
		// :::: Export 
		// Unused because JPanel has no built-in DnD functionality and so the start of 
		//	Dragging has to be added manually with a DragSource object
		@Override		public int getSourceActions( JComponent c) {return MOVE;}
		@Override		public Transferable createTransferable( JComponent c) {	return null;}
		@Override 		public void exportDone( JComponent c, Transferable t, int action) {}
		
		
		// :::: Import
		@Override
		public boolean canImport( TransferSupport support) {
			for( DataFlavor df : support.getDataFlavors()) {
				if( df == FLAVOR) {
					Point p = support.getDropLocation().getDropPoint();
					
					int tabIndex = -1;
					
					// Step 1: Determine which, if any tab you're hovering over
					for( int i = 0; i < root.getTabCount(); ++i) {
						Rectangle rect = root.getBoundsAt(i);
						if( rect.contains(p)) {
							tabIndex = i;
							break;
						}
					}
					
					if( tabIndex == -1) {
						dragMode = DragMode.DRAG_INTO_CONTENT;
					}
					else {
						dragMode = DragMode.DRAG_INTO_TAB;
					}
					dragTab = tabIndex;
					
					root.repaint();
					return true;
				}
			}
			
			
			return false;
		}
		
		@Override
		public boolean importData( TransferSupport support) {
			
			try {
				OFTransferable trans = 
						(OFTransferable)support.getTransferable().getTransferData(FLAVOR);

				trans.parent.removeContainer( trans.panel);
				addContainer(trans.panel, dragTab);
				
				dragMode = DragMode.NOT_DRAGGING;
				root.repaint();
				return true;
			} catch (UnsupportedFlavorException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}

		// :::: DragGestureListener
		@Override
		public void dragGestureRecognized(DragGestureEvent evt) {
			// Determine which component is being Dragged (all registerred components
			//	should be OmniBars and if they're not this object wouldn't know what
			//	to do with them anyway)
			for( OmniContainer container : containers) {
				if( evt.getComponent() == container.bar) {					
					OFTransferable oftrans = new OFTransferable( context, container);
					Transferable trans = (Transferable)oftrans;
					dragging = container;

					// Set the cursor and start the drag action
					Cursor cursor = DragSource.DefaultMoveDrop;
					int action = evt.getDragAction();
					if( action == DnDConstants.ACTION_MOVE)
						cursor = DragSource.DefaultMoveDrop;
					
					RenderSettings set = new RenderSettings();
					set.workspace = master.getCurrentWorkspace();
					
					dragSource.startDrag(
							evt, 
							cursor, 
							master.getRenderEngine().renderImage( set),
							new Point(10,10), 
							trans, 
							this);
				}
			}
			
		}

		// DragSourceListener
		@Override		public void dragDropEnd(DragSourceDropEvent evt) {
			// Drag out of current frame
			if( evt.getDropAction() == TransferHandler.NONE && containers.contains(dragging)) {
				if( containers.size() > 1) {
					FrameManager fm = master.getFrameManager();
					removeContainer(dragging);
					fm.containerToFrame(dragging, evt.getLocation());
				}
			}
			dragging = null;
		}
		@Override		public void dragEnter(DragSourceDragEvent arg0) {}
		@Override		public void dragOver(DragSourceDragEvent arg0) {}
		@Override		public void dropActionChanged(DragSourceDragEvent arg0) {}
		@Override
		public void dragExit(DragSourceEvent arg0) {
			if( dragMode != DragMode.NOT_DRAGGING) {
				dragMode = DragMode.NOT_DRAGGING;
				root.repaint();
			}
		}


	}
}
