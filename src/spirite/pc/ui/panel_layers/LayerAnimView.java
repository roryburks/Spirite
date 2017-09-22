package spirite.pc.ui.panel_layers;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

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
	
	List<AnimationSchemePanel> panels = new ArrayList<>();
	
	public LayerAnimView(MasterControl master) {
		this.master = master;
		InitComponents();
		master.addWorkspaceObserver(this);
	}
	
	private void InitComponents() {
    	this.setLayout(new GridLayout());
		this.add(tree);
	}

	private void Rebuild() {
		for( AnimationSchemePanel panel : panels) {
			panel.Rebuild();
		}
	}
	
	// AnimationStructureObserver
	@Override
	public void animationAdded(AnimationStructureEvent evt) {
		AnimationSchemePanel newPanel = new AnimationSchemePanel((FixedFrameAnimation)evt.getAnimation());
		panels.add(newPanel);
		tree.AddRoot( tree.new LeafNode(newPanel));
	}

	@Override
	public void animationRemoved(AnimationStructureEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void animationChanged(AnimationStructureEvent evt) {
		this.Rebuild();
		
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
