package spirite.pc.ui.panel_layers;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.AnimationView;
import spirite.base.image_data.AnimationView.MAnimationViewObserver;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.pc.ui.Transferables;
import spirite.pc.ui.generic.BetterTree;
import spirite.pc.ui.generic.BetterTree.BTNode;
import spirite.pc.ui.generic.BetterTree.DnDBinding;
import spirite.pc.ui.generic.BetterTree.DropDirection;

public class LayerAnimView extends JPanel implements MAnimationStructureObserver, MWorkspaceObserver, MAnimationViewObserver {
	private final MasterControl master;
	private final BetterTree tree = new BetterTree();
	private ImageWorkspace ws;
	
	//private final List<AnimationSchemePanel> panels = new ArrayList<>();
	
	private final JScrollPane scroll;
	
	public LayerAnimView(MasterControl master) {
		this.master = master;
		scroll = new JScrollPane(tree);
		InitComponents();
		master.addWorkspaceObserver(this);
		
		ws = master.getCurrentWorkspace();
		if( ws != null) {
			ws.getAnimationManager().addAnimationStructureObserver(this);;
			ws.getAnimationManager().getView().addAnimationViewObserver(this);
			tree.setRootBinding( new NodeBinding(ws.getAnimationManager().getView().getRoot()));
		}
		
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
		//scroll.getVerticalScrollBar().getUI().s
		
	}
	
	private class NodeBinding implements DnDBinding {
		final Node node;
		
		NodeBinding( Node node) {
			this.node = node;
		}

		@Override
		public Transferable buildTransferable() {
			return new Transferables.NodeTransferable(node);
		}

		@Override
		public Image drawCursor() {
			return null;
		}

		@Override
		public DataFlavor[] getAcceptedDataFlavors() {
			return flavors;
		}

		@Override
		public void interpretDrop(Transferable trans, DropDirection direction) {
			AnimationView av = ws.getAnimationManager().getView();
			Node nodeToMove = null;
			try {
				nodeToMove = ((Transferables.NodeTransferable)trans.getTransferData(flavors[0])).node;
			} catch (Exception e) {
				e.printStackTrace();
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
			
		}
	}
	private final DataFlavor[] flavors = new DataFlavor[] {Transferables.NodeTransferable.FLAVOR};
	
	private void InitComponents() {
    	this.setLayout(new GridLayout());
		this.add(scroll);
	}

	private void Rebuild() {
		tree.ClearRoots();
		if( ws != null) {
			
			List<Node> nodes = ws.getAnimationManager().getView().getRoot().getAllNodesST( new NodeValidator() {
				@Override
				public boolean isValid(Node node) {
					return node instanceof AnimationNode;
				}
				
				@Override
				public boolean checkChildren(Node node) {
					return true;
				}
			});
			
			
			for( Node node : nodes) {
				AnimationNode anode = ((AnimationNode)node);
				
				BTNode btnode = tree.new LeafNode( new AnimationSchemePanel(master, anode));
				btnode.setDnDBindings( new NodeBinding(node));
				
				tree.AddRoot( btnode);
			}
		}
		tree.repaint();
	}
	
	// AnimationStructureObserver
	@Override
	public void animationAdded(AnimationStructureEvent evt) {

		// !!! DEBUG
		ws.getAnimationManager().getView().addNode(evt.getAnimation());
		// !!! DEBUG
//		AnimationSchemePanel newPanel = new AnimationSchemePanel(master, (FixedFrameAnimation)evt.getAnimation());
//		panels.add(newPanel);
//		tree.AddRoot( tree.new LeafNode(newPanel));
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
		if( ws != null) {
			tree.setRootBinding( null);
			ws.getAnimationManager().removeAnimationStructureObserver(this);
			ws.getAnimationManager().getView().removeAnimationViewObserver(this);
		}
		ws = selected;
		if( ws != null) {
			tree.setRootBinding( new NodeBinding(ws.getAnimationManager().getView().getRoot()));
			ws.getAnimationManager().addAnimationStructureObserver(this);
			ws.getAnimationManager().getView().addAnimationViewObserver(this);
		}
		
		// !!! DEBUG
		for( Animation a : ws.getAnimationManager().getAnimations())
			ws.getAnimationManager().getView().addNode(a);
		// !!! DEBUG
		
		SwingUtilities.invokeLater( new Runnable() {
			
			@Override
			public void run() {
				Rebuild();	
			}
		});
	}

	// :::: MAnimationViewObserver
	@Override
	public void viewChanged() {
		Rebuild();
	}
}
