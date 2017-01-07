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
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

import spirite.brains.MasterControl;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.ui.FrameManager.FrameType;

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

		root = new OmniTabbedFrame();
		root.setTabPlacement(JTabbedPane.TOP);
		
		//
		transferHandler = new OFTransferHandler(this);
		root.setTransferHandler(transferHandler);
		
		// Create TabbedPane
		this.add( root);

		
		// Create the panel of the given type
		addPanel( type);	
		
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
				if( dragTab == -1) {
					g.drawRect(0, 0, getWidth()-1, getHeight()-1);
				}
				else {
					
					Rectangle r = root.getTabComponentAt(dragTab).getBounds();
					
	                Graphics2D g2 = (Graphics2D) g;
	                Stroke oldStroke = g2.getStroke();
					g2.setColor( Color.BLACK);
					g2.setStroke( new BasicStroke(3));
					g2.drawRect( r.x, r.y, r.width, r.height);
					g2.setStroke( oldStroke);
				}
			}
			
		}
	}
	
	
	/*** 
	 * Adds Panel of the given FrameType
	 */
	public void addPanel( FrameType type) {
		JPanel panel = master.getFrameManager().createOmniPanel(type);
		
		if( panel == null) return;
		
		root.addTab("Test", panel);
		
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
	
	
	public class OmniBar extends JPanel implements MouseListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public OmniBar( String title) {
			add( new JLabel(title));
			this.setOpaque(false);
			addMouseListener(this);
		}

		/***
		 * Required so that the custom tab component behaves like a tab.
		 */
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
	private class OmniContainer {
		JPanel panel;
		OmniBar bar;
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
			this.panel = panel;
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
		implements DragGestureListener
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		protected DragSource dragSource;
		protected List<DragGestureRecognizer> dgrs = new ArrayList<>();
		protected DropTarget dropTarget;
		private OmniFrame context;
		
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
					
					Component c = root.getComponentAt( support.getDropLocation().getDropPoint());
					
					
					// !!!! Note: This is very ugly and I'm not sure that this is guarenteed
					//	to work in all Java implementations past and pressent, but 
					//	since JTabbedPane lacks any "getTabContainer" option or similar
					//	this is what I have to do
					if( c == containers.get(0).bar.getParent()) {
						// Determine which (if any) tab you're hovered over
						Point p = SwingUtilities.convertPoint(
								root, 
								support.getDropLocation().getDropPoint(), 
								c);
						
						Component tab = c.getComponentAt(p);
						
						

						dragMode = DragMode.DRAG_INTO_TAB;
						
						// Determine which tab is being dragged over
						int i = 0;
						int cnt = root.getTabCount();
						dragTab = -1;
						for( i = 0; i < cnt; ++i) {
							if( tab == root.getTabComponentAt(i)) {
								dragTab = i;
							}
						}
						
						root.repaint();
						return true;
					}

				}
			}
			
			
			return false;
		}
		
		@Override
		public boolean importData( TransferSupport support) {
			dragMode = DragMode.NOT_DRAGGING;
			root.repaint();
			return true;
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
							null);
				}
			}
			
		}
	}
}
