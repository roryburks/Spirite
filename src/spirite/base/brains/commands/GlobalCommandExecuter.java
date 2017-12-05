package spirite.base.brains.commands;

import spirite.base.brains.MasterControl;
import spirite.base.brains.SettingsManager;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.file.LoadEngine;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.IMedium.InternalImageTypes;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.hybrid.HybridHelper;
import spirite.pc.ui.dialogs.Dialogs;
import spirite.pc.ui.omni.FrameManager;
import spirite.pc.ui.panel_work.WorkPanel.View;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** 
 * global.* Command Executer
 * 
 * These are abstract or top-level commands such as "Save", "Undo", "Copy", 
 * "Paste" and other things which have a higher context than manipulating 
 * data within a single Workspace
 */
public class GlobalCommandExecuter implements CommandExecuter {
	private final MasterControl master;
	private ImageWorkspace currentWorkspace;
	
	final Map<String, Runnable> commandMap = new HashMap<>();
	public GlobalCommandExecuter(MasterControl master) {
		this.master = master;
		
		SettingsManager settingsManager = master.getSettingsManager();
		Dialogs dialog = master.getDialogs();
		LoadEngine loadEngine = master.getLoadEngine();
		RenderEngine renderEngine = master.getRenderEngine();
		FrameManager frameManager = master.getFrameManager();
		ToolsetManager toolset = master.getToolsetManager();
		
		commandMap.put("save_image",() -> {
    		if( currentWorkspace == null)
    			return;
    		
        	File f=currentWorkspace.getFile();

        	if( currentWorkspace.hasChanged() || f == null) {
	        	if( f == null)
	        		f = dialog.pickFileSave();
	        	
	        	if( f != null) {
	        		master.saveWorkspace(currentWorkspace, f);
	        		settingsManager.setWorkspaceFilePath(f);
	        	}
        	}
		});
		commandMap.put("save_image_as", () -> {
			File f = dialog.pickFileSave();
			
			if( f != null) {
				master.saveWorkspace(currentWorkspace, f);
			}
		});
		commandMap.put("new_image", () -> {dialog.promptNewImage();});
		commandMap.put("open_image", () -> {
			File f =dialog.pickFileOpen();
			
			if( f != null) {
	        	loadEngine.openFile( f);
			}
		});
		commandMap.put("export", () -> {
			File f = dialog.pickFileExport();
			
			if( f != null) {
				master.exportWorkspaceToFile( currentWorkspace, f);
			}
		});
		commandMap.put("export_as", commandMap.get("export"));
		commandMap.put("copy", () -> {
			SelectionEngine selectionEngine= currentWorkspace.getSelectionEngine();
			if( currentWorkspace == null) return;
			Node selected = currentWorkspace.getSelectedNode();
			
			if( selected == null) commandMap.get("copyVisible").run();; 
			
			if(currentWorkspace.getSelectedNode() != null &&
					selectionEngine.getSelection() != null) {
    			// Copies the current selection to the Clipboard

    	    	AtomicReference<IImage> img = new AtomicReference<>(null);
				if( selectionEngine.isLifted()) {
					// Copies straight from the lifted data
					img.set(selectionEngine.getLiftedData().readonlyAccess());
				}
				else {
					BuildingMediumData building = currentWorkspace.buildActiveData();
					
					if( building == null) {
		    	    	RenderSettings settings = new RenderSettings(
		    	    			renderEngine.getNodeRenderTarget(selected));
		
		    	    	RawImage nodeImg = renderEngine.renderImage(settings);
						img.set(selectionEngine.getSelection().liftRawImage(nodeImg, 0, 0));
					}
					else {
						building.doOnBuiltData((built) -> {
	    					img.set(selectionEngine.getSelection().liftSelectionFromData(built));
						});
					}
				}
				
				HybridHelper.imageToClipboard(img.get());
			}
			else {
    			// Copies the current selected node to the Clipboard
    	    	GroupTree.Node node = currentWorkspace.getSelectedNode();

    	    	RenderSettings settings = new RenderSettings(
    	    			renderEngine.getNodeRenderTarget(node));

    	    	RawImage img = renderEngine.renderImage(settings);
    	    	
    	    	HybridHelper.imageToClipboard(img);
			}
		});
		commandMap.put("copyVisible", () -> {
			if( currentWorkspace == null) return;
			
			// Copies the current default render to the Clipboard
			RenderSettings settings = new RenderSettings(
					renderEngine.getDefaultRenderTarget(currentWorkspace));
			
			// Should be fine to send Clipboard an internal reference since once
			//	rendered, the RenderEngine's cache should be immutable
	    	RawImage img = renderEngine.renderImage(settings);
	    	
	    	RawImage lifted =  currentWorkspace.getSelectionEngine().getSelection()
	    			.liftRawImage(img, 0, 0);

	    	HybridHelper.imageToClipboard(lifted);
		});
		commandMap.put("cut", () -> {
			commandMap.get("copy").run();
			
			master.executeCommandString("draw.clearLayer");
		});
		commandMap.put("paste",() -> {
			RawImage bi = HybridHelper.imageFromClipboard();
			if( bi == null) return;
			
    		if( currentWorkspace == null) {
	    		// Create new Workspace from Pasted Data
    			master.createWorkspaceFromImage(bi, true);
    		}
    		else if( currentWorkspace.buildActiveData() == null){
    			//	Paste Data as new layer
    			currentWorkspace.addNewSimpleLayer(currentWorkspace.getSelectedNode(), bi, "Pasted Image", InternalImageTypes.NORMAL);
    		}
    		else {
    			// Paste Data onto Selection Engine (current selected Data)
    			int ox = 0, oy=0;
    			
    			View zoom = frameManager.getZoomerForWorkspace(currentWorkspace);

    			int min_x = zoom.stiX(0);
    			int min_y = zoom.stiY(0);
    			
    			Node node = currentWorkspace.getSelectedNode();
    			if( node != null) {
    				
    				ox = Math.max(min_x,node.getOffsetX());
    				oy = Math.max(min_y,node.getOffsetY());
    			}

    			currentWorkspace.getSelectionEngine().imageToSelection(bi, ox, oy);
    			
    			toolset.setSelectedTool(Tool.BOX_SELECTION);
    		}
		});
		commandMap.put("pasteAsLayer", () -> {
			RawImage bi = HybridHelper.imageFromClipboard();
			if( bi == null) return;
			
    		if( currentWorkspace == null) {
	    		// Create new Workspace from Pasted Data
    			master.createWorkspaceFromImage(bi, true);
    		}
    		else {
    			//	Paste Data as new layer
    			currentWorkspace.addNewSimpleLayer(currentWorkspace.getSelectedNode(), bi, "Pasted Image", InternalImageTypes.NORMAL);
    		}
		});
		commandMap.put("toggleGL", () -> {
			settingsManager.setGL( !settingsManager.glMode());
		});
		commandMap.put("toggleGLPanel", () -> {
			frameManager.getWorkPanel().setGL(!frameManager.getWorkPanel().isGLPanel());
		});
		
		commandMap.put("debug1", () -> {
			toolset.getToolSettings(Tool.PEN).setValue("alpha", 0.5f);
		});
	}
	

	@Override public List<String> getValidCommands() {
		return new ArrayList<>(commandMap.keySet());
	}

	@Override
	public String getCommandDomain() {
		return "global";
	}

	@Override
	public boolean executeCommand(String command, Object extra) {
		Runnable runnable = commandMap.get(command);
		currentWorkspace = master.getCurrentWorkspace();
		
		if( runnable != null) {
			runnable.run();
			return true;
		}
		else
			return false;
	}
}