package spirite.base.brains.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.file.AnimIO;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.animations.FixedFrameAnimation;
import spirite.base.image_data.animations.RigAnimation;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.pc.ui.dialogs.NewLayerDPanel.NewLayerHelper;

public class AnimationContextCommandExecuter implements CommandExecuter {
	private final MasterControl master;
	
	Animation animation;
	ImageWorkspace workspace;
	final Map<String, Runnable> commandMap = new HashMap<>();
	
	public AnimationContextCommandExecuter(MasterControl master) {
		this.master = master;
		initCommands();
	}
	
	private void initCommands() {
		commandMap.put("delete", () -> {
			workspace.getAnimationManager().removeAnimation(animation);
		});
	}

	@Override
	public List<String> getValidCommands() {
		return new ArrayList<>(commandMap.keySet());
	}

	@Override
	public String getCommandDomain() {
		return "animation";
	}

	@Override
	public boolean executeCommand(String command, Object extra) {
		workspace = master.getCurrentWorkspace();
		if( workspace == null) return false;
		
		animation = (extra instanceof Animation) ? (Animation) extra : workspace.getAnimationManager().getSelectedAnimation();
		
		Runnable run = commandMap.get(command);
		if( run != null) {
			run.run();
			return true;
		}
		
		return false;
	}
}
