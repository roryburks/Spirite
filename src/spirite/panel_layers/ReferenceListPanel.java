package spirite.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ReferenceManager;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.image_data.layers.Layer;
import spirite.image_data.layers.RigLayer.Part;
import spirite.ui.UIUtil;

public class ReferenceListPanel extends JPanel 
	implements MReferenceObserver, MWorkspaceObserver,  ListCellRenderer<Layer>
{
	private final ReferenceSchemePanel context;
	private final MasterControl master;
	private ImageWorkspace workspace;

	private final Color bgColor = new Color(64,64,64);
	private final Color bgColorActive = new Color(64,64,64);
	private final Color helperColor = new Color( 90,90,64);
	
	private final JLabel helperLabel = new JLabel();
	private final JList<Layer> refList = new JList<>();
	private final DefaultListModel<Layer> model = new DefaultListModel<>();

	public ReferenceListPanel(MasterControl master, ReferenceSchemePanel context) {
		setBackground(bgColor);
//		setBackground(null);
		this.context = context;
		this.master = master;
		
		initComponents();
		
		master.addWorkspaceObserver(this);
		workspace = master.getCurrentWorkspace();
		if( workspace != null)
			workspace.getReferenceManager().addReferenceObserve(this);
	
		refList.setModel(model);
		refList.setCellRenderer( this);
		

		this.setDropTarget( new RLPDropTarget());
		this.setTransferHandler(new RLPTransfer());
	}
	
	
	class RLPCellPanel extends JPanel {
		JLabel label;
		RLPCellPanel() {
			GroupLayout layout = new GroupLayout(this);
			label = new JLabel();

			layout.setVerticalGroup( layout.createSequentialGroup()
				.addComponent(label, 20,20,20)
			);
			layout.setHorizontalGroup( layout.createParallelGroup()
				.addComponent(label)
			);
			this.setLayout(layout);
		}
	}
	private final RLPCellPanel renderComponent = new RLPCellPanel();
	
	@Override
	public Component getListCellRendererComponent(
			JList<? extends Layer> list, Layer value, int index,
			boolean isSelected, boolean cellHasFocus) 
	{
		if( value == null) renderComponent.label.setText("Base Image");
		else  renderComponent.label.setText("Layer: " + index);
		return renderComponent;
	}
	
	private class RLPDropTarget extends DropTarget {
		@Override
		public synchronized void drop(DropTargetDropEvent dtde) {
			try {
				Node node = ((LayerTreePanel.NodeTransferable)dtde.getTransferable().getTransferData(LayerTreePanel.FLAVOR)).node;
				
				if( node instanceof LayerNode) {
					Layer layer = ((LayerNode) node).getLayer();
					
					master.getCurrentWorkspace().getReferenceManager().addReference(
							layer, 
							0);
				}
				
			} catch (UnsupportedFlavorException | IOException e) {}
			
						
			super.drop(dtde);
		}
		@Override
		public synchronized void dragEnter(DropTargetDragEvent dtde) {
			System.out.println("DE");
			super.dragEnter(dtde);
		}
	}
	
	private class RLPTransfer extends TransferHandler {
		public RLPTransfer() {
/*			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer( 
							tree, 
							DnDConstants.ACTION_COPY_OR_MOVE, 
							this);*/
		}
		
		
		 
		@Override
		public boolean canImport(TransferSupport support) {
			System.out.println(support.getDataFlavors());
			return super.canImport(support);
		}
	}
	
	private void initComponents() {
		GroupLayout layout = new GroupLayout(this);
		
		helperLabel.setText("<html><h2>Drag nodes here to add them to the reference section, drag out to remove.");
		helperLabel.setForeground(helperColor);

		layout.setVerticalGroup( layout.createParallelGroup()
				.addComponent(refList)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(helperLabel, 0, 0, Short.MAX_VALUE)
			)
		);
		layout.setHorizontalGroup( layout.createParallelGroup()
				.addComponent(refList)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(helperLabel, 100,100,100)
				)
		);
		this.setLayout(layout);
	}

	@Override
	public void referenceStructureChanged(boolean hard) {
		if( hard) {
			ReferenceManager refMan = workspace.getReferenceManager();

			model.clear();
			for( Layer layer : refMan.getFrontList()) {
				model.addElement(layer);
			}
			model.addElement(null);
			for( Layer layer : refMan.getBackList()) {
				model.addElement(layer);
			}
		}
	}

	@Override
	public void toggleReference(boolean referenceMode) {
		setBackground( referenceMode ? bgColorActive : bgColor);
	}

	// MWorkspaceObserver
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( workspace != null) {
			workspace.getReferenceManager().removeReferenceObserve(this);
		}
		workspace = selected;
		if( workspace != null) {
			workspace.getReferenceManager().addReferenceObserve(this);
			
		}
	}

	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	

	// :::: WorkspaceObserver inherited from NodeTree
/*	@Override
	public void currentWorkspaceChanged(ImageWorkspace current, ImageWorkspace previous) {
		if( workspace != null) {
			workspace.removeReferenceObserve(this);
		}
		
		super.currentWorkspaceChanged(current, previous);

		if( workspace == null)
			nodeRoot = null;
		else {
			nodeRoot = workspace.getReferenceRoot();
			workspace.addReferenceObserve(this);
		}
		constructFromRoot();
	}

	
	// :::: ContentTree
	@Override
	protected boolean importAbove( Transferable trans, TreePath path) {
		try {
			Node tnode = nodeFromTransfer(trans);
			Node context = nodeFromPath(path);
			
			if( workspace.verifyReference(tnode)) {
				workspace.moveAbove(tnode, context);
			}
			else {
				Node toAdd = workspace.shallowDuplicateNode(tnode);
				workspace.addReferenceNode(toAdd, context.getParent(), context);
			}
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
			return false;
		}
	}
	@Override
	protected boolean importBelow( Transferable trans, TreePath path) {
		try {
			Node tnode = nodeFromTransfer(trans);
			Node context = nodeFromPath(path);
			
			if( workspace.verifyReference(tnode)) {
				workspace.moveBelow(tnode, context);
			}
			else  {
				Node toAdd = workspace.shallowDuplicateNode(tnode);
				workspace.addReferenceNode(toAdd, context.getParent(), context.getNextNode());
			}
			
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
			return false;
		}
	}
	@Override
	protected boolean importInto( Transferable trans, TreePath path, boolean top) {
		try {
			Node tnode =nodeFromTransfer(trans);
			Node context = nodeFromPath(path);
			Node before = null;

			if( workspace.verifyReference(tnode))
				workspace.moveInto(tnode, (GroupNode) context, top);
			else {	
				Node toAdd =  workspace.shallowDuplicateNode(tnode);
				if(top && !context.getChildren().isEmpty())
					before = context.getChildren().get(0);
	
				workspace.addReferenceNode(toAdd, context, before);
			}
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
			return false;
		}
	}
	@Override
	protected boolean importOut( Transferable trans) {
			try {
				Node tnode = nodeFromTransfer(trans);
				
				if( !workspace.verifyReference(tnode)) {
					Node toAdd = workspace.shallowDuplicateNode(tnode);
					workspace.addReferenceNode(toAdd, workspace.getReferenceRoot(), null);
				}
				return true;
			} catch (Exception e) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree):" + e.getMessage());
				return false;
			}
	}
	
	@Override
	protected boolean importClear(TreePath path) {
		Node node = nodeFromPath(path);
		workspace.clearReferenceNode(node);
		constructFromRoot();
		return super.importClear(path);
	}
	
	// Prevents a self-over from being interpreted as an importClear
	@Override
	protected boolean importSelf(Transferable trans, TreePath path) {
		return true;
	}


	// :::: MReferenceObserver
	@Override
	public void referenceStructureChanged(boolean hard) {
		if( hard)
			constructFromRoot();
		
	}


	@Override
	public void toggleReference(boolean referenceMode) {}
	
	// :::: NodeTree
	@Override
	void cleanup() {
		if( workspace != null)
			workspace.removeReferenceObserve(this);
		super.cleanup();
	}*/
}
