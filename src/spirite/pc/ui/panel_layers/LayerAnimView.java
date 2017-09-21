package spirite.pc.ui.panel_layers;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.ImageWorkspace;
import spirite.pc.ui.generic.BetterTree;
import spirite.base.image_data.animation_data.FixedFrameAnimation;

public class LayerAnimView extends JPanel implements MAnimationStructureObserver, MWorkspaceObserver {
	MasterControl master;
	BetterTree tree = new BetterTree();
	
	public LayerAnimView(MasterControl master) {
		this.master = master;
		InitComponents();
		master.addWorkspaceObserver(this);
	}
	
	private void InitComponents() {
    	this.setLayout(new GridLayout());
		this.add(tree);
	}

	// AnimationStructureObserver
	@Override
	public void animationAdded(AnimationStructureEvent evt) {
		tree.AddRoot( tree.new LeafNode(new JButton("SN")));
		//tree.AddRoot( tree.new LeafNode(new AnimationSchemePanel((FixedFrameAnimation)evt.getAnimation())));
	}

	@Override
	public void animationRemoved(AnimationStructureEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void animationChanged(AnimationStructureEvent evt) {
		// TODO Auto-generated method stub
		
	}

	// WorkspaceObserver
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		selected.getAnimationManager().addAnimationStructureObserver(this);
		
	}

	@Override
	public void newWorkspace(ImageWorkspace newWorkspace) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeWorkspace(ImageWorkspace newWorkspace) {
		// TODO Auto-generated method stub
		
	}
}
