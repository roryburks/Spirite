package spirite.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.TransferHandler;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.ui.FrameManager.FrameType;

/***
 * 
 * TODO: So apparently as late as Java 1.8, JDialogs are still memory
 * 	leaks in that they are JNI roots that are never cleared up by the GC.
 * (Although some research suggests this only exists in the JDK, not the JRE)
 * 	So at some point (in addition to figuring out a unintrusive way
 * 	to make components de-link Observers) I have set up the frame
 * 	manager to re-use OmniFrames or at least void them out to 
 * 	prevent the memory leak from mattering at all.
 * 
 * @author RoryBurks
 *
 */
public class OmniFrame extends JPanel
{	
	private static final long serialVersionUID = 1L;
	private final MasterControl master;
	private final FrameManager frameManager;
	
	// Components
	private final OFTransferHandler transferHandler = new OFTransferHandler(this);
	private final List<OmniContainer> containers = new ArrayList<>();
	private JTabbedPane root = new OmniTabbedFrame();
	
	// Drag UI States
	private enum DragMode {
		NOT_DRAGGING,
		DRAG_INTO_TAB,
		DRAG_INTO_CONTENT
	}
	private DragMode dragMode = DragMode.NOT_DRAGGING;
	private int dragIndex = 0;
	
	OmniFrame( MasterControl master, FrameType type) {
		this.master = master;
		this.frameManager = master.getFrameManager();
		construct();
		
		// Create the panel of the given type
		addPanel( type);	
		
	}
	
	OmniFrame(MasterControl master, OmniContainer container) {
		this.master = master;
		this.frameManager = master.getFrameManager();
		construct();
		
		addContainer(container, -1);
	}

	private void construct() {
		root.setTabPlacement(JTabbedPane.TOP);
		root.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		//getGlassPane().setVisible(true);
		//getGlassPane().addMouseListener(this);
		//getGlassPane().addMouseMotionListener(this);;
		
		root.setTransferHandler(transferHandler);
		
		// Create TabbedPane
		this.setLayout(new GridLayout());
		this.add( root);
		
	}
	
	/** The TabbedPane Container for all component panels */
	public class OmniTabbedFrame extends JTabbedPane {
		private static final long serialVersionUID = 1L;

		OmniTabbedFrame() {
			super();
		}

		@Override
		public void paintComponent( Graphics g) {
			super.paintComponent(g);

			// Draw Graphics related to Docking
			switch( dragMode) {
			case DRAG_INTO_TAB:
				Rectangle r = root.getBoundsAt(dragIndex);
				
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
	
	
	/** Adds Panel of the given FrameType. */
	public void addPanel( FrameType type) {
		OmniComponent panel = master.getFrameManager().createOmniComponent(type);
		
		if( panel == null) return;
		
		root.addTab("tab", panel);
		
		OmniBar bar = new OmniBar( type.getName(), type);
		root.setTabComponentAt(root.getTabCount()-1, bar);
		
		containers.add(new OmniContainer(panel, bar, type));
		transferHandler.refreshGestureRecognizers();
	}
	
	/** Get all the FrameTypes contained in the OmniFrame. */
	public List<FrameType> getContainedFrameTypes() {
		List<FrameType> list = new ArrayList<FrameType>();

		for( OmniContainer container : containers) {
			list.add( container.type);
		}
		
		return list;
	}
	
	/*** @return True if the OmniPanel contains any frame of the given Type */
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
			frameManager.triggereClose(this);
		}
	}
	private void addContainer( OmniContainer toAdd, int index) {
		if( index == -1) {
			root.add(toAdd.component);
			containers.add(toAdd);
			index = containers.size()-1;
		}
		else {
			root.add(toAdd.component, index);
			containers.add(index,toAdd);
		}
		toAdd.bar = new OmniBar( toAdd.type.getName(), toAdd.type);
		root.setTabComponentAt(index, toAdd.bar);
		root.setSelectedIndex(index);
		transferHandler.refreshGestureRecognizers();
	}
	
	// Called by FrameManager
	void triggerCleanup() {
		for( OmniContainer container : containers) {
			container.component.onCleanup();
		}
	}
	
	
	/** Custom Tab Component */
	public class OmniBar extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;
		
		private final JLabel label;
		private final JPanel iconPanel;
		final ImageIcon icon;
		
		
		public OmniBar( String title, FrameType type) {
			icon = FrameManager.getIconForType(type);
			
			label = new JLabel(title);
			iconPanel = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					g.drawImage(icon.getImage(),0,0,null);
					
				}
			};
			iconPanel.setOpaque(false);
			
			GroupLayout layout = new GroupLayout(this);
			
			layout.setHorizontalGroup( layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addComponent(iconPanel,24,24,24)
					.addComponent(label, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				)
			);
			
			layout.setVerticalGroup(layout.createParallelGroup( Alignment.CENTER)
				.addComponent(iconPanel,24,24,24)
				.addComponent(label));
			
			this.setLayout(layout);
			
			label.setFont( new Font("Tahoma", Font.PLAIN, 10));
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
	
	/** */
	static class OmniContainer {
		OmniComponent component;
		OmniBar bar;	// It's possible that this shouldn't be here.  It helps
						//  simplify some internal code, but it is tied closely to
						//  the particular OmniFrame that it is currently housed
						//  in whereas the OmniContainer class is more abstract
		FrameType type;
		
		OmniContainer( OmniComponent component, OmniBar bar, FrameType type) {
			this.component = component;
			this.bar = bar;
			this.type = type;
		}
	}
	
	/** 
	 * Because Components often bind into semi-global contexts with observers and
	 *	listeners, they will need to remove those links when the component is closed
	 *	so that they can properly be caught by the GC.
	 */
	public static abstract class OmniComponent extends JPanel{
		public void onCleanup() {}
	}
	
	
	/** Transferable Object storing the data which Component is moving 
	 * and what its parent it.  */
	private static class OFTransferable implements Transferable {
		private final OmniFrame parent;
		private final OmniContainer panel;
		
		OFTransferable( OmniFrame parent, OmniContainer container) {
			this.parent = parent;
			this.panel = container;
		}
		
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if( flavor.equals(FLAVOR)) return this;
			else throw new UnsupportedFlavorException(flavor);
		}
		@Override public DataFlavor[] getTransferDataFlavors() {return flavors;}
		@Override public boolean isDataFlavorSupported(DataFlavor flavor) {return flavor.equals(FLAVOR);}
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
		protected final DragSource dragSource;
		protected final List<DragGestureRecognizer> dgrs = new ArrayList<>();
		
		// Used only for dragging a container out to a new Frame, otherwise
		//	the Transferable object is used
		private OmniContainer dragging = null;
		
		public OFTransferHandler(OmniFrame context) {
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
					dragIndex = tabIndex;
					
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

				// Move the container from its old spot to its new (unless
				//	you're trying to move a single-frame OmniPanel into itself)
				if( trans.parent != OmniFrame.this || containers.size() > 1) {
					OmniContainer temp = trans.panel;
					trans.parent.removeContainer( trans.panel);
					addContainer(temp, dragIndex);
				}

				dragMode = DragMode.NOT_DRAGGING;
				root.repaint();
				return true;
			} catch (UnsupportedFlavorException | IOException e) {
				MDebug.handleError(ErrorType.STRUCTURAL, e, "Tried to import unsupported Data in OmniFrame (shouldn't have been flagged as importable).");
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
					OFTransferable oftrans = new OFTransferable( OmniFrame.this, container);
					Transferable trans = (Transferable)oftrans;
					dragging = container;

					// Set the cursor and start the drag action
					Cursor cursor = DragSource.DefaultMoveDrop;
					int action = evt.getDragAction();
					if( action == DnDConstants.ACTION_MOVE)
						cursor = DragSource.DefaultMoveDrop;
					
					
					BufferedImage image = new BufferedImage(128,24,Globals.BI_FORMAT);
					Graphics g = image.getGraphics();
					
					g.setColor( new Color(128,128,128,128));
					g.fillRect(0, 0, image.getWidth(), image.getHeight());
					if( container.bar.icon != null)
						g.drawImage( container.bar.icon.getImage(), 0, 0, null);
					g.setColor( Color.BLACK);
					g.drawString(container.type.getName(), 24, 16);
					g.dispose();
					
					dragSource.startDrag(
							evt, 
							cursor, 
							image,
							new Point(10,10), 
							trans, 
							this);
				}
			}
			
		}

		// :::: DragSourceListener
		@Override		public void dragEnter(DragSourceDragEvent arg0) {}
		@Override		public void dragOver(DragSourceDragEvent arg0) {}
		@Override		public void dropActionChanged(DragSourceDragEvent arg0) {}
		@Override		public void dragDropEnd(DragSourceDropEvent evt) {

			if( evt.getDropAction() == TransferHandler.NONE && containers.contains(dragging)) {
				// When you drag into a component that has its own DnD handling, you can
				//	get a false negative in which importData is never called, so it's 
				//	handled here
				if( root.contains( evt.getLocation())) {
					if( containers.size() > 1) {
						removeContainer( dragging);
						addContainer( dragging, dragIndex);
					}
				}
				
				// Drag a tab out of its frame
				else if( containers.size() > 1) {
					FrameManager fm = master.getFrameManager();
					removeContainer(dragging);
					fm.containerToFrame(dragging, evt.getLocation());
				}
			}
			dragMode = DragMode.NOT_DRAGGING;
			dragging = null;
		}
		@Override
		public void dragExit(DragSourceEvent arg0) {
			if( dragMode != DragMode.NOT_DRAGGING) {
				dragMode = DragMode.NOT_DRAGGING;
				root.repaint();
			}
		}
	}
}
