package spirite.pc.ui.panel_layers;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.ImageWorkspace;
import spirite.pc.ui.generic.BetterTree;
import spirite.base.image_data.animation_data.FixedFrameAnimation;

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
			for( Animation anim : ws.getAnimationManager().getAnimations()) {
				tree.AddRoot( tree.new LeafNode(new AnimationSchemePanel(master, (FixedFrameAnimation)anim)));
				System.out.println("Anim");
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
