package spirite.base.brains.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.util.glmath.Rect;

/** CommandExecuter for "select.*" commands.  Commands which affect the 
 * selection form.     */
public class SelectionCommandExecuter implements CommandExecuter {
	private final MasterControl master;
	private final Map<String, Runnable> commandMap = new HashMap<>();
	
	
	// For simplicity's sake, stored before executing the command
	private ImageWorkspace workspace;
	private SelectionEngine selectionEngine;
	
	public SelectionCommandExecuter(MasterControl master) {
		this.master = master;
		
		commandMap.put("all", () -> {
			selectionEngine.setSelection( selectionEngine.buildRectSelection(
					new Rect(0,0,workspace.getWidth(), workspace.getHeight())));

		});
		commandMap.put("none", () -> {
			selectionEngine.setSelection(null);
		});
		commandMap.put("invert", () -> {
			selectionEngine.setSelection( selectionEngine.invertSelection(selectionEngine.getSelection()));
		});
	}
	
	@Override
	public List<String> getValidCommands() {
		return new ArrayList<>(commandMap.keySet());
	}

	@Override
	public String getCommandDomain() {
		return "select";
	}

	@Override
	public boolean executeCommand(String command) {
		Runnable runnable = commandMap.get(command);
		
		if( runnable != null) {
			workspace = master.getCurrentWorkspace();
			if( workspace != null) {
				selectionEngine = workspace.getSelectionEngine();
				runnable.run();
			}
			return true;
		}
		else
			return false;
	}
	
}