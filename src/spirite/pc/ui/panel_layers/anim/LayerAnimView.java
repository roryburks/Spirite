package spirite.pc.ui.panel_layers.anim;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.AnimationView;
import spirite.base.image_data.AnimationView.MAnimationViewObserver;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.gui.hybrid.SPanel;
import spirite.gui.hybrid.SScrollPane;
import spirite.pc.ui.Transferables;
import spirite.pc.ui.components.BetterTree;
import spirite.pc.ui.components.BetterTree.BTNode;
import spirite.pc.ui.components.BetterTree.BranchingNode;
import spirite.pc.ui.components.BetterTree.DnDBinding;
import spirite.pc.ui.components.BetterTree.DropDirection;
import spirite.pc.ui.components.ResizeContainerPanel;
import spirite.pc.ui.components.ResizeContainerPanel.ContainerOrientation;
import spirite.pc.ui.panel_layers.LayersPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class LayerAnimView extends SPanel implements MAnimationStructureObserver, MWorkspaceObserver, MAnimationViewObserver {
	private final MasterControl master;
	private final LayersPanel context;
	private final BetterTree tree = new BetterTree();
	private ImageWorkspace ws;
	private final ResizeContainerPanel container;
	
	//private final List<AnimationSchemePanel> panels = new ArrayList<>();
	
	private final SScrollPane scroll;
	
	public LayerAnimView(MasterControl master, LayersPanel context) {
		this.master = master;
		this.context = context;
		
		scroll = new SScrollPane(tree);
		container = new ResizeContainerPanel( scroll, ContainerOrientation.VERTICAL);
		container.addPanel(0,200, -1, new BasicLayerTree(master));
//		container.addPanel( 0, 100, -1, new LayerTreePanel(master,context));
		
		InitComponents();
		master.addWorkspaceObserver(this);
		tree.setRootBinding( rootBinding);
		
		ws = master.getCurrentWorkspace();
		master.addTrackingObserver(MAnimationStructureObserver.class, this);
		master.addTrackingObserver(MAnimationViewObserver.class, this);
		
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
		//scroll.getVerticalScrollBar().getUI().s
		
	}
	
	private final DnDBinding rootBinding = new DnDBinding() {
		@Override
		public void interpretDrop(Transferable trans, DropDirection direction) {
			AnimationView av = ws.getAnimationManager().getView();
			GroupNode root = av.getRoot();
			try {
				Node nodeToMove = null;
				if( trans.isDataFlavorSupported(flavors[0])) 
					nodeToMove = ((Transferables.NodeTransferable)trans.getTransferData(flavors[0])).node;
				else if( trans.isDataFlavorSupported(flavors[1])) {
					Animation anim = ((Transferables.AnimationTransferable)trans.getTransferData(flavors[1])).animation;
					nodeToMove = av.addNode(anim);
				}
				
				switch( direction) {
				case ABOVE:
					av.moveInto(nodeToMove, root, true);
					break;
				case BELOW:
				case INTO:
					av.moveInto(nodeToMove, root, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override public DataFlavor[] getAcceptedDataFlavors() {
			return flavors;
		}
		@Override public Image drawCursor() {return null;}
		@Override public Transferable buildTransferable() {return null;}
		@Override public void dragOut() {}
	};
	
	private class NodeBinding implements DnDBinding {
		final Node node;
		
		NodeBinding( Node node) {
			this.node = node;
		}

		@Override public Image drawCursor() {return null;}
		@Override
		public Transferable buildTransferable() {
			return new Transferables.NodeTransferable(node);
		}


		@Override
		public DataFlavor[] getAcceptedDataFlavors() {
			return flavors;
		}

		@Override
		public void interpretDrop(Transferable trans, DropDirection direction) {
			if( ws == null)
				return;
			
			AnimationView av = ws.getAnimationManager().getView();
			try {
				Node nodeToMove = null;
				if( trans.isDataFlavorSupported(flavors[0])) 
					nodeToMove = ((Transferables.NodeTransferable)trans.getTransferData(flavors[0])).node;
				else if( trans.isDataFlavorSupported(flavors[1])) {
					Animation anim = ((Transferables.AnimationTransferable)trans.getTransferData(flavors[1])).animation;
					nodeToMove = av.addNode(anim);
				}

				switch( direction) {
				case ABOVE:
					av.moveAbove(nodeToMove, node);
					break;
				case BELOW:
					av.moveBelow(nodeToMove, node);
					break;
				case INTO:
					av.moveInto(nodeToMove, (GroupNode)node, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override public void dragOut() {
			if( ws != null) {
				ws.getAnimationManager().getView().RemoveNode(node);
			}
		}
	}
	private final DataFlavor[] flavors = new DataFlavor[] {Transferables.NodeTransferable.FLAVOR, Transferables.AnimationTransferable.FLAVOR};
	
	private void InitComponents() {
    	this.setLayout(new GridLayout());
		this.add(container);
	}

	private void Rebuild() {
		tree.ClearRoots();
		if( ws != null) {
			
			for( Node node : ws.getAnimationManager().getView().getRoot().getChildren())
				RebuildSub(node, null);
		}
		tree.repaint();
	}
	private void RebuildSub(Node node, BranchingNode branch) {
		BTNode toAdd = null;
		
		if( node instanceof GroupNode) {
			toAdd = tree.new BranchingNode(new JLabel(node.getName()));
			for( Node child : node.getChildren())
				RebuildSub( child, (BranchingNode)toAdd);
		}
		else if( node instanceof LayerNode) {
			toAdd = tree.new LeafNode(new JLabel(node.getName()));
		}
		else if( node instanceof AnimationNode) {
			AnimationNode anode = ((AnimationNode)node);
			// TODO
			// TODO
			// TODO
//
//			if( anode.getAnimation() instanceof FixedFrameAnimation)
//				toAdd = tree.new LeafNode( new FFAnimationSchemePanel(master, anode));
//			else if( anode.getAnimation() instanceof RigAnimation)
//				toAdd = tree.new LeafNode( new RigAnimationSchemePanel(master, anode));
		}
		if( toAdd == null)
			return;

		toAdd.setUserObject(node);
		toAdd.setDnDBindings( new NodeBinding(node));
		
		if( branch == null)
			tree.AddRoot( toAdd);
		else
			branch.AddNode(toAdd);
	}
	
	
	// AnimationStructureObserver
	@Override
	public void animationAdded(AnimationStructureEvent evt) {
		Rebuild();
	}

	@Override
	public void animationRemoved(AnimationStructureEvent evt) {
		Rebuild();
	}

	@Override
	public void animationChanged(AnimationStructureEvent evt) {
		Rebuild();
	}

	// WorkspaceObserver
	@Override public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override public void removeWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		ws = selected;
		SwingUtilities.invokeLater( () ->{Rebuild();});
	}

	// :::: MAnimationViewObserver
	@Override
	public void viewChanged() {
		Rebuild();
	}
	@Override
	public void viewSelectionChange(Node selected) {
		Rebuild();
	}
}
