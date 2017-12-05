package spirite.pc.ui.panel_layers.anim;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.gui.hybrid.SPanel;
import spirite.pc.ui.ContextMenus;
import spirite.pc.ui.Transferables;
import spirite.pc.ui.components.BetterTree;
import spirite.pc.ui.components.BetterTree.BTNode;
import spirite.pc.ui.components.BetterTree.BranchingNode;
import spirite.pc.ui.components.BetterTree.DropDirection;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

;

public class BasicLayerTree extends SPanel implements MWorkspaceObserver, MImageObserver, MAnimationStructureObserver {
	private final MasterControl master;
	private ImageWorkspace ws = null;
	
	private final BetterTree tree = new BetterTree();
	
	public BasicLayerTree( MasterControl master) {
		this.master = master;
		
		master.addWorkspaceObserver(this);
		currentWorkspaceChanged( master.getCurrentWorkspace(), null);
		
    	this.setLayout(new GridLayout());
		this.add(tree);

		tree.addMouseListener( new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( e.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu cmenu = ContextMenus.cmenu;
					cmenu.removeAll();
					BTNode btnode = tree.getNodeAtPoint(e.getPoint());

					if( btnode == null)
						return;
					
					Object usrObj = btnode.getUserObject();
					if( usrObj instanceof AnimationNode) {
						final Animation animation = ((AnimationNode) usrObj).getAnimation();
						ContextMenus.constructMenu(
								cmenu, 
								ContextMenus.constructSchemeForAnimation(ws, animation),
								(e2) -> master.executeCommandString(e2.getActionCommand(), animation));
					}
					else {
						final Node node = ((Node)usrObj);
						ContextMenus.constructMenu(
								cmenu, 
								ContextMenus.constructSchemeForNode(ws, node),
								(e2) -> master.executeCommandString(e2.getActionCommand(), node));
					}
					
					cmenu.show(tree, e.getX(), e.getY());
				}
			}
		});
		
		Rebuild();
	}
	
	private void Rebuild() {
		tree.ClearRoots();
		if( ws != null) {
			BranchingNode branch = tree.new BranchingNode(new JLabel("Animations"));
			
			for( Animation anim : ws.getAnimationManager().getAnimations()) {
				BTNode toAdd = tree.new LeafNode( new JLabel(anim.getName()));
				toAdd.setDnDBindings( new AnimBinding( anim));
				branch.AddNode(toAdd);
			}
			tree.AddRoot( branch);
			
			for( Node node : ws.getRootNode().getChildren()) 
				RebuildSub(node, null);
		}
	}
	private void RebuildSub( Node node, BranchingNode branch) {
		BTNode nodeToAdd = null;
		if( node instanceof GroupNode) {
			BranchingNode nextBranch = tree.new BranchingNode(new JLabel(node.getName()));
			for( Node child : node.getChildren())
				RebuildSub(child, nextBranch);
			nodeToAdd = nextBranch;
			nextBranch.SetExpanded(node.isExpanded());
		}
		else {
			nodeToAdd = tree.new LeafNode( new JLabel(node.getName()));
		}
		
		nodeToAdd.setDnDBindings( new NodeBinding(node));
		if( branch == null)
			tree.AddRoot(nodeToAdd);
		else
			branch.AddNode(nodeToAdd);
	}

	private class AnimBinding implements BetterTree.DnDBinding {
		private final Animation animation;
		AnimBinding( Animation animation) {this.animation = animation;}
		@Override
		public Transferable buildTransferable() {
			return new Transferables.AnimationTransferable(animation);
		}
		@Override public Image drawCursor() {return null;}
		@Override public DataFlavor[] getAcceptedDataFlavors() {return new DataFlavor[] {};}
		@Override	public void interpretDrop(Transferable trans, DropDirection direction) {}
		@Override public void dragOut() {}
	}
	private class NodeBinding implements BetterTree.DnDBinding {
		private final Node node;
		NodeBinding( Node node) {this.node = node;}
		@Override
		public Transferable buildTransferable() {
			return new Transferables.NodeTransferable(node);
		}
		@Override public Image drawCursor() {return null;}
		@Override public DataFlavor[] getAcceptedDataFlavors() {return new DataFlavor[] {};}
		@Override	public void interpretDrop(Transferable trans, DropDirection direction) {}
		@Override public void dragOut() {}
	}

	// :: MWorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( ws != null) {
			ws.getAnimationManager().removeAnimationStructureObserver(this);;
			ws.removeImageObserver(this);
		}
		ws = selected;
		if( ws!= null) {
			ws.getAnimationManager().addAnimationStructureObserver(this);
			ws.addImageObserver(this);
		}
		
		Rebuild();
	}

	// :: MImageObserver
	@Override public void imageChanged(ImageChangeEvent evt) {}
	@Override
	public void structureChanged(StructureChangeEvent evt) {
		Rebuild();
	}

	// :: MAnimationStructureObserver
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



}
