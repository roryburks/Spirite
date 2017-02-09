package spirite.brains;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.UnsupportedDataTypeException;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.MUtil.TransferableImage;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.brains.ToolsetManager.Tool;
import spirite.dialogs.Dialogs;
import spirite.file.LoadEngine;
import spirite.file.SaveEngine;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.ReferenceManager;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.RectSelection;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.layers.Layer;
import spirite.ui.FrameManager;

/***
 * Master Control is little more than a container for all the various components 
 * which handle the grand internals of the program.  
 * 
 * Note: Though most UI components will need full access to MasterControl, giving
 * it to too many internal components is probably indicative of backwards design.
 * 
 * !!!! NOTE: I have made the decision to allow null workspace selection (particularly
 * 	when none are open). This opens up a lot of scrutiny to be placed on UI components.
 * 
 * @author Rory Burks
 *
 */
public class MasterControl
	implements MImageObserver 
{
	// Components
    private final HotkeyManager hotkeys;
    private final ToolsetManager toolset;
    private final SettingsManager settingsManager;
    private final CacheManager cacheManager;
    private final FrameManager frameManager;
    private final PaletteManager palette;	// Requires SettingsManager
    private final RenderEngine renderEngine;// Require CacheManager
    private final SaveEngine saveEngine;
    private final LoadEngine loadEngine;
    private final Dialogs dialog;

    private final List<ImageWorkspace> workspaces = new ArrayList<>();
    private ImageWorkspace currentWorkspace = null;
    private final CommandExecuter executers[];
    

    public MasterControl() {
        settingsManager = new SettingsManager();
        hotkeys = new HotkeyManager();
        toolset = new ToolsetManager();
        cacheManager = new CacheManager();
        renderEngine = new RenderEngine( this);	
        palette = new PaletteManager( this);
        loadEngine = new LoadEngine(this);
        saveEngine = new SaveEngine(this);
        dialog = new Dialogs(this);
        frameManager = new FrameManager( this);
        

        // As of now I see no reason to dynamically construct this with a series 
        //	of addCommandExecuter and removeCommandExecuter methods.  I could make
        //	command execution modular that way, but it'd invite forgotten GC links.
        executers = new CommandExecuter[] {
        	new GlobalCommandExecuter(),
        	new RelativeWorkspaceCommandExecuter(),
        	new SelectionCommandExecuter(),
        	toolset,
        	palette,
        	frameManager,
        	frameManager.getRootFrame(),
        	dialog
        };
    }


    // :::: Getters/Setters
    public HotkeyManager getHotekyManager() {
        return hotkeys;
    }
    public ToolsetManager getToolsetManager() {
        return toolset;
    }
    public PaletteManager getPaletteManager() {
    	return palette;
    }
    public ImageWorkspace getCurrentWorkspace() {
   		return currentWorkspace;
    }
    public List<ImageWorkspace> getWorkspaces() {
    	return new ArrayList<>(workspaces);
    }
    public FrameManager getFrameManager() {
    	return frameManager;
    }
    public RenderEngine getRenderEngine(){
    	return renderEngine;
    }
    public SettingsManager getSettingsManager() {
    	return settingsManager;
    }
    public CacheManager getCacheManager() {
    	return cacheManager;
    }
    public SaveEngine getSaveEngine() {
    	return saveEngine;
    }
    public LoadEngine getLoadEngine() {
    	return loadEngine;
    }
    public Dialogs getDialogs() {
    	return dialog;
    }
    
    
    public void saveWorkspace( ImageWorkspace workspace, File f) {
    	if( workspace == null || f == null) return;
    	saveEngine.saveWorkspace( workspace, f );
		workspace.fileSaved(f);
		saveEngine.removeAutosaved(workspace);
		saveEngine.triggerAutosave(workspace, 5*60, 10);	// Autosave every 5 minutes
    }
    
    // :::: Workspace API
    public void closeWorkspace( ImageWorkspace workspace) {
    	closeWorkspace(workspace, true);
    }
    public void closeWorkspace( ImageWorkspace workspace, boolean promptSave) {
    	int i = workspaces.indexOf(workspace);
    	
    	if( i == -1) {
    		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to remove a workspace that is not being tracked.");
    		return;
    	}
    	
    	if( promptSave && workspace.hasChanged() ) {
    		if( promptSave(workspace) == JOptionPane.CANCEL_OPTION)
    			return;
    	}
    	
    	// Remove the workspace
    	workspace.cleanup();
    	workspaces.remove(i);
    	triggerRemoveWorkspace(workspace);
    	
    	if(  workspaces.size() > i) {
    		setCurrentWorkpace( workspaces.get(i));
    	}else if( i > 0){
    		setCurrentWorkpace( workspaces.get(i-1));
    	} else {
    		setCurrentWorkpace(null);
    	}
    	
    }
    
    /** Prompt the User if he wants to save a file, then if he . 
     * 
     * @return 
     * YES_OPTION if saved
     * <br>NO_OPTION if User doesn't want to save
     * <br>CANCEL_OPTION if User cancels
     * */
    public int promptSave( ImageWorkspace workspace ) {
		// Prompt the User to Save the file before closing if it's 
		//	changed and respond accordingly.
    	int ret = JOptionPane.showConfirmDialog(
    			null,
    			"Save " + workspace.getFileName() + " before closing?",
    			"Closing " + workspace.getFileName(),
    			JOptionPane.YES_NO_CANCEL_OPTION,
    			JOptionPane.QUESTION_MESSAGE
    			);
    	
    	if( ret == JOptionPane.CANCEL_OPTION)
    		return ret;
    	
    	if( ret == JOptionPane.YES_OPTION) {
    		File f = workspace.getFile();
    		
    		if( f == null)
    			f = dialog.pickFileSave();
    		
    		if( f != null) {
    			saveEngine.saveWorkspace(workspace, workspace.getFile());
    			saveEngine.removeAutosaved(workspace);
    			settingsManager.setWorkspaceFilePath(f);
    			return ret;
    		}
    		else
    			return JOptionPane.CANCEL_OPTION;
    	}
    	
    	// NO_OPTION
    	return ret;
    }

    /***
     * Makes the given workspace the currently selected workspace.
     * 
     * Note: if the workspace is not already managed by MasterControl it will add it to
     * 	management, but you should really be using addWorkspace then.
     */
    public void setCurrentWorkpace( ImageWorkspace workspace) {
    	if( currentWorkspace == workspace)
    		return;
    	
    	if( workspace != null && !workspaces.contains(workspace)) {
    		MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to assign current workspace to a workspace that MasterControl isn't tracking.");
    		
    		addWorkpace(workspace, false);
    	}
    	
    	ImageWorkspace previous = currentWorkspace;
    	currentWorkspace = workspace;
    	triggerWorkspaceChanged( workspace, previous);
    }
    

    /***
     * Called when you want to add a Workspace that has been algorithmically constructed
     * 	such as with the LoadEngine, rather than making a new one with a default layer.
     * @return 
     */
	public ImageWorkspace addWorkpace(ImageWorkspace workspace, boolean select) {
		workspaces.add(workspace);
		triggerNewWorkspace(workspace);
		
		workspace.addImageObserver(this);
		
		if( select || currentWorkspace == null) {
			setCurrentWorkpace(workspace);
		}
		
		return workspace;
	}
	
	public ImageWorkspace createWorkspaceFromImage( BufferedImage image, boolean select) {
		ImageWorkspace workspace = new ImageWorkspace(this);
		if( image != null)
			workspace.addNewSimpleLayer(null, image, "Base Image");
		workspace.finishBuilding();
		
		this.addWorkpace(workspace, select);
		return workspace;
	}
    

    public void newWorkspace( int width, int height) {newWorkspace(width,height,new Color(0,0,0,0), true);}
    public void newWorkspace( int width, int height, Color color, boolean selectOnCreate) {
    	ImageWorkspace ws = new ImageWorkspace( this);
    	ws.addNewSimpleLayer(null, width, height, "Background", color);
    	
    	workspaces.add( ws);
    	ws.addImageObserver( this);
    	
    	triggerNewWorkspace(ws);
    	if( selectOnCreate || currentWorkspace == null) {
    		setCurrentWorkpace( ws);
    	}
    }
    
    
    
    public void executeCommandString( String command) {
    	String space = (command == null)?"":command.substring(0, command.indexOf("."));
    	String subCommand = command.substring(space.length()+1);
    	
    	boolean executed = false;
    	boolean attempted = false;
    	
    	for( CommandExecuter executer : executers) {
    		if( executer.getCommandDomain().equals(space)) {
    			attempted = true;
    			if(executer.executeCommand(subCommand))
    				executed = true;
    		}
    	}
    	if( !executed) {
    		if( attempted)
    			MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unrecognized command:" + command);
    		else
    			MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unrecognized command domain:" + space);
    	}

    	
    }
    public List<String> getAllValidCommands() {
    	List<String> list = new ArrayList<>();
    	for( CommandExecuter executer : executers) {
    		String domain = executer.getCommandDomain();
    		List<String> commands = executer.getValidCommands();
    		
    		for( String command : commands) {
    			list.add( domain + "." + command);
    		}
    	}
    	
    	return list;
    }

    public interface CommandExecuter {
    	public abstract List<String> getValidCommands();
    	public String getCommandDomain();
    	public boolean executeCommand( String command);
    }
    
    class GlobalCommandExecuter implements CommandExecuter {
    	final Map<String, Runnable> commandMap = new HashMap<>();
    	GlobalCommandExecuter() {
    		commandMap.put("save_image", new Runnable() {
				@Override public void run() {
		    		if( currentWorkspace == null)
		    			return;
		    		
		        	File f=currentWorkspace.getFile();

		        	if( currentWorkspace.hasChanged() || f == null) {
			        	if( f == null)
			        		f = dialog.pickFileSave();
			        	
			        	if( f != null) {
			        		saveWorkspace(currentWorkspace, f);
			        		settingsManager.setWorkspaceFilePath(f);
			        	}
		        	}
				}
			});
    		commandMap.put("save_image_as", new Runnable() {
				@Override public void run() {
					File f = dialog.pickFileSave();
					
					if( f != null) {
						saveWorkspace(currentWorkspace, f);
					}
				}
			});
    		commandMap.put("new_image", new Runnable() {
				@Override public void run() {
		    		dialog.promptNewImage();
				}
			});
    		commandMap.put("debug_color", new Runnable() {
				@Override public void run() {
		    		dialog.promptDebugColor();
				}
			});
    		commandMap.put("open_image", new Runnable() {
    			@Override public void run() {
	    			File f =dialog.pickFileOpen();
	    			
	    			if( f != null) {
	    	        	loadEngine.openFile( f);
	    			}
    			}
    		});
    		commandMap.put("export", new Runnable() {@Override public void run() {
				File f = dialog.pickFileExport();
				
				if( f != null) {
					exportWorkspaceToFile( currentWorkspace, f);
				}
			}});
    		commandMap.put("export_as", commandMap.get("export"));
    		commandMap.put("copy", new Runnable() {@Override public void run() {
    			if( currentWorkspace == null ||
    				currentWorkspace.getSelectedNode() == null) return; 
    			
    			if(currentWorkspace.getSelectedNode() instanceof LayerNode &&
    				currentWorkspace.getSelectionEngine().getSelection() != null) {
        			// Copies the current selection to the Clipboard
    				
    				// TODO: Implement for GroupNode

	    	    	BufferedImage img;
    				if( currentWorkspace.getSelectionEngine().isLifted())
    					img = currentWorkspace.getSelectionEngine().getLiftedImage().access();
    				else {
    					img = currentWorkspace.getSelectionEngine().getBuiltSelection()
    						.liftSelectionFromData(currentWorkspace.buildActiveData());
    				}
    				
	    	    	TransferableImage transfer = new TransferableImage(img);
	    	    	
	    	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	    	c.setContents(transfer, null);
    			}
    			else {
	    			// Copies the current selected node to the Clipboard
	    	    	GroupTree.Node node = currentWorkspace.getSelectedNode();
	
	    	    	RenderSettings settings = new RenderSettings(
	    	    			renderEngine.getNodeRenderTarget(node));
	
	    	    	BufferedImage img = renderEngine.renderImage(settings);
	    	    	TransferableImage transfer = new TransferableImage(img);
	    	    	
	    	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	    	c.setContents(transfer, null);
    			}
    		}});
    		commandMap.put("copyVisible", new Runnable() {@Override public void run() {
    			if( currentWorkspace == null) return;
    			
    			// Copies the current default render to the Clipboard
    			RenderSettings settings = new RenderSettings(
    					renderEngine.getDefaultRenderTarget(currentWorkspace));
//    			settings.workspace = currentWorkspace;
    			
    			// Should be fine to send Clipboard an internal reference since once
    			//	rendered, the RenderEngine's cache should be immutable
    	    	BufferedImage img = renderEngine.renderImage(settings);
    	    	TransferableImage transfer = new TransferableImage(img);
    	    	
    	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    	    	c.setContents(transfer, null);
    		}});
    		commandMap.put("cut", new Runnable() {@Override public void run() {
    			commandMap.get("copy").run();
    			
    			MasterControl.this.executeCommandString("draw.clearLayer");
    		}});
    		commandMap.put("paste", new Runnable() {@Override public void run() {
    			BufferedImage bi = MUtil.imageFromClipboard();
    			if( bi == null) return;
    			
	    		if( currentWorkspace == null) {
		    		// Create new Workspace from Pasted Data
	    			createWorkspaceFromImage(bi, true);
	    		}
	    		else if( currentWorkspace.buildActiveData() == null){
	    			//	Paste Data as new layer
	    			currentWorkspace.addNewSimpleLayer(currentWorkspace.getSelectedNode(), bi, "Pasted Image");
	    		}
	    		else {
	    			// Paste Data onto Selection Engine (current selected Data)
	    			int ox = 0, oy=0;
	    			
	    			Node node = currentWorkspace.getSelectedNode();
	    			if( node != null) {
	    				ox = node.getOffsetX();
	    				oy = node.getOffsetY();
	    			}
	    			
	    			currentWorkspace.getSelectionEngine().imageToSelection(bi, ox, oy);
	    			toolset.setSelectedTool(Tool.BOX_SELECTION);
	    		}
    		}});
    		commandMap.put("pasteAsLayer", new Runnable() {@Override public void run() {
    			BufferedImage bi = MUtil.imageFromClipboard();
    			if( bi == null) return;
    			
	    		if( currentWorkspace == null) {
		    		// Create new Workspace from Pasted Data
	    			createWorkspaceFromImage(bi, true);
	    		}
	    		else {
	    			//	Paste Data as new layer
	    			currentWorkspace.addNewSimpleLayer(currentWorkspace.getSelectedNode(), bi, "Pasted Image");
	    		}
    		}});
    	}
    	

		@Override public List<String> getValidCommands() {
			return new ArrayList<>(commandMap.keySet());
		}

		@Override
		public String getCommandDomain() {
			return "global";
		}

		@Override
		public boolean executeCommand(String command) {
			Runnable runnable = commandMap.get(command);
			
			if( runnable != null) {
				runnable.run();
				return true;
			}
			else
				return false;
		}
    	
    }

    class RelativeWorkspaceCommandExecuter implements CommandExecuter {
    	private final Map<String, Runnable> commandMap = new HashMap<>();
    	
    	// For simplicity's sake, workspaces are stored in the Class
    	//	scope and checked for non-null there before being passed to the
    	//	anonymous Runnable's
    	private ImageWorkspace workspace;
    	RelativeWorkspaceCommandExecuter() {
    		commandMap.put("undo", new Runnable() {@Override public void run() {
    			workspace.getUndoEngine().undo();
    		}});
    		commandMap.put("redo", new Runnable() {@Override public void run() {
				workspace.getUndoEngine().redo();
			}});
    		commandMap.put("toggle", new Runnable() {@Override public void run() {
				workspace.toggleQuick();
			}});
    		commandMap.put("shiftRight", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), 1, 0);
			}});
    		commandMap.put("shiftLeft", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), -1, 0);
			}});
    		commandMap.put("shiftDown", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), 0, 1);
    		}});
    		commandMap.put("shiftUp", new Runnable() {@Override public void run() {
				workspace.shiftData(workspace.getSelectedNode(), 0, -1);
			}});
    		commandMap.put("newLayerQuick", new Runnable() {@Override public void run() {
				workspace.addNewSimpleLayer(workspace.getSelectedNode(), 
						workspace.getWidth(), workspace.getHeight(), 
						"New Layer", new Color(0,0,0,0));
    		}});
    		commandMap.put("toggle_reference", new Runnable() {@Override public void run() {
					ReferenceManager rm = workspace.getReferenceManager();
					rm.setEditingReference(!rm.isEditingReference());
			}});
    		commandMap.put("clearLayer", new Runnable() {@Override public void run() {
				if(!workspace.getSelectionEngine().attemptClearSelection()) {
					// Note: transforms are irrelevant for this action, so 
					//	accessing handle directly is appropriate.
					BuiltImageData image = workspace.buildActiveData();
					if( image != null) 
						workspace.getDrawEngine().clear(image);
				}
			}});
    		commandMap.put("cropSelection", new Runnable() {@Override public void run() {
				Node node = workspace.getSelectedNode();
				SelectionEngine selectionEngine = workspace.getSelectionEngine();
				
				Selection selection = selectionEngine.getSelection();
				if( selection == null) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}

				Rectangle rect = selection.getBounds();
				rect.x += selectionEngine.getOffsetX();
				rect.y += selectionEngine.getOffsetY();
				
				workspace.cropNode(node, rect, false);
			}});
    		commandMap.put("autocroplayer", new Runnable() {@Override public void run() {
				// TODO: Will probably need fixing with new BuiltActiveDAta
				//	format
				Node node = workspace.getSelectedNode();
				
				if( node instanceof LayerNode) {
					Layer layer = ((LayerNode)node).getLayer();

					try {
						Rectangle rect;
						rect = MUtil.findContentBounds(
								layer.getActiveData().handle.deepAccess(),
								1, 
								false);
						rect.x += node.getOffsetX();
						rect.y += node.getOffsetY();
						workspace.cropNode((LayerNode) node, rect, true);
					} catch (UnsupportedDataTypeException e) {
						e.printStackTrace();
					}
				}
    		}});
    		commandMap.put("layerToImageSize", new Runnable() {@Override public void run() {
				Node node = workspace.getSelectedNode();
				
				if( node != null)
					workspace.cropNode(node, new Rectangle(0,0,workspace.getWidth(), workspace.getHeight()), false);
    		}});
    	}

		@Override public List<String> getValidCommands() {
			return new ArrayList<>(commandMap.keySet());
		}

		@Override
		public String getCommandDomain() {
			return "draw";
		}

		@Override
		public boolean executeCommand(String command) {
			Runnable runnable = commandMap.get(command);
			
			if( runnable != null) {
				if( currentWorkspace != null) {
					workspace = currentWorkspace;
					runnable.run();
				}
				return true;
			}
			else
				return false;
		}
    }
    
    private class SelectionCommandExecuter implements CommandExecuter {
    	private final Map<String, Runnable> commandMap = new HashMap<>();
    	
    	// For simplicity's sake, stored before executing the command
    	private ImageWorkspace workspace;
    	private SelectionEngine selectionEngine;
    	
    	public SelectionCommandExecuter() {
    		commandMap.put("all", new Runnable() {@Override public void run() {
    			selectionEngine.setSelection(
    					new RectSelection(workspace.getWidth(),workspace.getHeight()), 0, 0);
    		}});
    		commandMap.put("none", new Runnable() {@Override public void run() {
    			selectionEngine.unselect();
    		}});
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
				if( currentWorkspace != null) {
					workspace = currentWorkspace;
					selectionEngine = currentWorkspace.getSelectionEngine();
					runnable.run();
				}
				return true;
			}
			else
				return false;
		}
    	
    }
    
    private void exportWorkspaceToFile( ImageWorkspace workspace, File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	RenderSettings settings = new RenderSettings(
    			renderEngine.getDefaultRenderTarget(workspace));
    	BufferedImage bi = renderEngine.renderImage(settings);
    	
    	if( ext.equals("jpg") || ext.equals("jpeg")) {
    		// Remove Alpha Layer of JPG so that encoding works correctly
    		BufferedImage bi2 = bi;
    		bi = new BufferedImage( bi2.getWidth(), bi2.getHeight(), BufferedImage.TYPE_INT_RGB);
    		Graphics g = bi.getGraphics();
    		g.drawImage(bi2, 0, 0, null);
    		g.dispose();
    	}
    	
    	try {
			ImageIO.write( bi, ext, f);
			settingsManager.setImageFilePath(f);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Failed to Export file: " + e.getMessage());
			e.printStackTrace();
		}
    }
    
    
    // Properly implementing this will require a better understanding of Swing
    //	and AWT threads, but the idea is to lock the Program from terminating
    //	

    

    // ==== Observer Interfaces ====
    /***
     * A WorkspaceObserver watches for changes in which workspace is being 
     * actively selected, it doesn't watch for changes inside any Workspace
     * for that you need one of the various Observers in ImageWorkspace
     */
    public static interface MWorkspaceObserver {
        public void currentWorkspaceChanged(  ImageWorkspace selected,  ImageWorkspace previous);
        public void newWorkspace( ImageWorkspace newWorkspace);
        public void removeWorkspace( ImageWorkspace newWorkspace);
    }
    List<MWorkspaceObserver> workspaceObservers = new ArrayList<>();

    public void addWorkspaceObserver( MWorkspaceObserver obs) { workspaceObservers.add(obs);}
    public void removeWorkspaceObserver( MWorkspaceObserver obs) { workspaceObservers.remove(obs); }
    
    private void triggerWorkspaceChanged( ImageWorkspace selected, ImageWorkspace previous) {
    	for( MWorkspaceObserver obs : workspaceObservers) {
    		obs.currentWorkspaceChanged(selected, previous);
    	}
    	triggerImageStructureRefresh();
    	triggerImageRefresh();
    }
    private void triggerNewWorkspace(ImageWorkspace added) {
    	for( MWorkspaceObserver obs : workspaceObservers) {
    		obs.newWorkspace(added);
    	}
    }
    private void triggerRemoveWorkspace(ImageWorkspace removed) {
    	for( MWorkspaceObserver obs : workspaceObservers) {
    		obs.removeWorkspace(removed);
    	}
    }
    
    
    // :::: MCurrentImageObserver
    /***
     * A lot of components only ever draw the currently active image workspace
     * and redraws on any kind of status change.  For these, MCurrentImageObserver
     * is easier to use than a putting a MImageObserver on the current workspace
     *and changing it every time the workspace changes
     */
    public static interface MCurrentImageObserver {
    	public void imageRefresh();
    	public void imageStructureRefresh();
    }
    List<MCurrentImageObserver> cimageObservers = new ArrayList<>();

    public void addCurrentImageObserver( MCurrentImageObserver obs) { cimageObservers.add(obs);}
    public void removeCurrentImageObserver( MCurrentImageObserver obs) { cimageObservers.remove(obs); }

    private void  triggerImageRefresh() {
    	for( MCurrentImageObserver obs : cimageObservers) {
    		obs.imageRefresh();
    	}
    }
    private void  triggerImageStructureRefresh() {
    	for( MCurrentImageObserver obs : cimageObservers) {
    		obs.imageStructureRefresh();
    	}
    }
    

	// :::: MImageObserver
	@Override
	public void structureChanged(StructureChangeEvent evt) {
		triggerImageStructureRefresh();
	}

	@Override
	public void imageChanged( ImageChangeEvent evt) {
		triggerImageRefresh();
	}


}
