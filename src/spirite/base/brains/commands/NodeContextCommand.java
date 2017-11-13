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
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.animations.FixedFrameAnimation;
import spirite.base.image_data.animations.RigAnimation;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.pc.ui.dialogs.NewLayerDPanel.NewLayerHelper;

public class NodeContextCommand implements CommandExecuter {
	private final MasterControl master;
	
	Node node;
	ImageWorkspace workspace;
	final Map<String, Runnable> commandMap = new HashMap<>();
	
	public NodeContextCommand(MasterControl master) {
		this.master = master;
		initCommands();
	}
	
	private void initCommands() {
		commandMap.put("animfromgroup", () -> {
			GroupNode group = (GroupNode)node;
			
			String name = JOptionPane.showInputDialog("Enter name for new Animation:", group.getName());
			
			AnimationManager manager = workspace.getAnimationManager();
			manager.addAnimation(new FixedFrameAnimation(group, name, true));
		});
		commandMap.put("giffromgroup", () -> {
			GroupNode group = (GroupNode)node;
			try {
				AnimIO.exportGroupGif(group, new File("E:/test.gif"), 8);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		commandMap.put("animFromRig", () -> {
			SpriteLayer sprite = (SpriteLayer)((LayerNode)node).getLayer();
			
			String name = JOptionPane.showInputDialog("Enter name for new Animation:", node.getName());
			
			AnimationManager manager = workspace.getAnimationManager();
			manager.addAnimation(new RigAnimation((LayerNode)node, name));
		});
		commandMap.put("animinsert", () -> {
			GroupNode group = (GroupNode)node;
			AnimationManager manager = workspace.getAnimationManager();
			Animation anim  = manager.getSelectedAnimation();
			
			if( anim instanceof FixedFrameAnimation) 
				((FixedFrameAnimation) anim).importGroup(group, true);
		});
		commandMap.put("animBreakBind", () -> {
			// TODO
		});
		
		commandMap.put("newGroup", () -> {
			workspace.addGroupNode(node, "New Group");
		});
		commandMap.put("newLayer", () -> {
			NewLayerHelper helper = master.getDialogs().callNewLayerDialog(workspace);
			if( helper != null) {
				workspace.addNewSimpleLayer( node, 
						helper.width, helper.height, helper.name, helper.color.getRGB(), helper.imgType);
			}
		});
		commandMap.put( "duplicate", () -> {workspace.duplicateNode(node);});
		commandMap.put( "delete", () -> {workspace.removeNode(node);});
		commandMap.put( "mergeDown", () -> {workspace.mergeNodes( node.getNextNode(), (LayerNode) node);});
		commandMap.put( "newRig", () -> { workspace.addNewRigLayer(node, 1, 1, "rig", 0);});
		commandMap.put( "newPuppet", () -> {
			String name = JOptionPane.showInputDialog("Enter name for new Animation:", 
					workspace.getNonDuplicateName("puppetLayer"));
			workspace.addNewPuppetLayer( node, name);
		});
	}

	@Override
	public List<String> getValidCommands() {
		return new ArrayList<>(commandMap.keySet());
	}

	@Override
	public String getCommandDomain() {
		return "node";
	}

	@Override
	public boolean executeCommand(String command, Object extra) {
		workspace = master.getCurrentWorkspace();
		if( workspace == null) return false;
		
		node = (extra instanceof Node) ? (Node) extra : workspace.getSelectedNode();
		
		Runnable run = commandMap.get(command);
		if( run != null) {
			run.run();
			return true;
		}
		
		return false;
	}

}
