package spirite.pc.ui.panel_layers;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.pc.ui.generic.BetterTree;

public class LayerAnimView extends JPanel implements MAnimationStructureObserver, MWorkspaceObserver {
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
		if( ws != null)
			ws.getAnimationManager().addAnimationStructureObserver(this);;
		
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
		//scroll.getVerticalScrollBar().getUI().s
	}
	
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
				tree.AddRoot( tree.new LeafNode(new AnimationSchemePanel(master, anode)));
			}
		}
		tree.repaint();
	}
	
	// AnimationStructureObserver
	@Override
	public void animationAdded(AnimationStructureEvent evt) {
		Rebuild();

		// !!! DEBUG
		ws.getAnimationManager().getView().addNode(evt.getAnimation());
		// !!! DEBUG
//		AnimationSchemePanel newPanel = new AnimationSchemePanel(master, (FixedFrameAnimation)evt.getAnimation());
//		panels.add(newPanel);
//		tree.AddRoot( tree.new LeafNode(newPanel));
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
		if( ws != null)
			ws.getAnimationManager().removeAnimationStructureObserver(this);
		ws = selected;
		if( ws != null)
			ws.getAnimationManager().addAnimationStructureObserver(this);
		
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
}
