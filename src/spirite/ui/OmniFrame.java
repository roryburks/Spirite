package spirite.ui;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.TransferHandler;

import spirite.brains.MasterControl;
import spirite.panel_layers.LayersPanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.ui.FrameManager.FrameType;

public class OmniFrame extends JDialog
{
	private List<OmniContainer> containers = new ArrayList<>();
	
	private MasterControl master;
	
	private OFTransferHandler transferHandler;
	
	private JTabbedPane root;
	
	public OmniFrame( MasterControl master, FrameType type) {
		this.master = master;

		//
		transferHandler = new OFTransferHandler();
		this.setTransferHandler(transferHandler);
		
		// Create TabbedPane
		root = new JTabbedPane( JTabbedPane.TOP);
		this.add( root);

		
		// Create the panel of the given type
		addPanel( type);	
	}
	
	
	/*** 
	 * Adds Panel of the given FrameType
	 */
	public void addPanel( FrameType type) {
		OmniPanel panel = master.getFrameManager().createOmniPanel(type);
		
		if( panel == null) return;
		
		root.addTab("Test", panel);
		
		OmniBar bar = new OmniBar( type.getName());
		root.setTabComponentAt(root.getTabCount()-1, bar);
		
		containers.add(new OmniContainer(panel, bar));
		transferHandler.refreshGestureRecognizers();
	}
	
	/***
	 * @return All the FrameTypes contained in the OmniFrame
	 */
	public List<FrameType> getContainedFrameTypes() {
		List<FrameType> list = new ArrayList<FrameType>();

		for( OmniContainer container : containers) {
			list.add( container.panel.getFrameType());
		}
		
		return list;
	}
	
	/***
	 * @return True if the OmniPanel contains any frame of the given Type
	 */
	public boolean containsFrameType( FrameType type) {
		for( OmniContainer container : containers) {
			if( container.panel.getFrameType() == type)
				return true;
		}
		
		return false;
	}
	
	
	/***
	 * An omnipanel is just a JPanel that has a special identifier that tells WHAT
	 * kind of panel it is.
	 */
	public static class OmniPanel extends JPanel {
		public FrameType getFrameType() {return FrameType.BAD;}
	}
	public class OmniBar extends JPanel implements MouseListener {
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
		OmniPanel panel;
		OmniBar bar;
		
		OmniContainer( OmniPanel panel, OmniBar bar) {
			this.panel = panel;
			this.bar = bar;
		}
	}
	
	
	/***
	 * 
	 */
	private static class OFTransferable implements Transferable {
		OmniFrame parent;
		OmniPanel panel;
		
		OFTransferable( OmniFrame parent, OmniPanel panel) {
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
	private final static DataFlavor FLAVOR = 
			new DataFlavor( OFTransferable.class, "OmniPanel");
	private static DataFlavor flavors[] = {FLAVOR};
	
	
	/***     
	 * The Transfer Handler handles all of the Drag-and-Drop code relating to
	 *  OmniPanels
	 */
	protected class OFTransferHandler extends TransferHandler 
		implements DragGestureListener
	{
		protected DragSource dragSource;
		protected List<DragGestureRecognizer> dgrs = new ArrayList<>();
		protected DropTarget dropTarget;
		
		public OFTransferHandler() {
			dragSource = DragSource.getDefaultDragSource();
		}
		
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
		@Override
		public int getSourceActions( JComponent c) {
			System.out.println("get: " + c);
			return MOVE;
		}
		
		@Override
		public Transferable createTransferable( JComponent c) {
			System.out.println("create: " + c);
			return null;
		}
		
		@Override 
		public void exportDone( JComponent c, Transferable t, int action) {

			System.out.println("get: " + c);
		}
		
		// :::: Import
		@Override
		public boolean canImport( TransferSupport support) {
			
			for( DataFlavor df : support.getDataFlavors()) {
				System.out.println(df.getHumanPresentableName());
			}
			
			
			return true;
		}
		
		@Override
		public boolean importData( TransferSupport support) {
			return true;
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent evt) {
			System.out.println( evt.getComponent());
			
			for( OmniContainer container : containers) {
				if( evt.getComponent() == container.bar) {
					System.out.println( containers.indexOf(container));
					
					Transferable trans = (Transferable)null;
				}
			}
			
		}
	}
}
