package spirite.brains;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.dialogs.Dialogs;
import spirite.file.LoadEngine;
import spirite.file.SaveEngine;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.RenderEngine;
import spirite.image_data.RenderEngine.RenderSettings;
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
    

    public MasterControl() {
        hotkeys = new HotkeyManager();
        toolset = new ToolsetManager();
        settingsManager = new SettingsManager();
        cacheManager = new CacheManager();
        frameManager = new FrameManager( this);
        renderEngine = new RenderEngine( this);	
        palette = new PaletteManager( this);
        loadEngine = new LoadEngine(this);
        saveEngine = new SaveEngine(this);
        dialog = new Dialogs(this);
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
    	String space = (command == null)?"":command.substring(0, command.indexOf(".")+1);
    	switch( space) {
    	case "global.":
            globalHotkeyCommand(command.substring("global.".length()));
            break;
    	case "toolset.":
    		toolset.setSelectedTool(command.substring("toolset.".length()));    		
            break;
    	case "palette.":
        	palette.performCommand(command.substring("palette.".length()));
    		break;
    	case "frame.":
        	frameManager.performCommand(command.substring("frame.".length()));
        	break;
    	case "context.":
            frameManager.getRootFrame().contextualCommand(command.substring("context.".length()));
            break;
    	case "draw.":
        	if( currentWorkspace != null) {
        		currentWorkspace.executeDrawCommand( command.substring("draw.".length()));
        	}
        	break;
       	default:
            	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Command String prefix: " + command);
    	}
    }
    
    /** Performs the given hotkey command string (should be of "global." focus). */
    private void globalHotkeyCommand( String command) {
    	
    	switch( command) {
    	case "save_image":{
    		if( currentWorkspace == null)
    			break;
    		
        	File f=currentWorkspace.getFile();

        	if( currentWorkspace.hasChanged() || f == null) {
	        	if( f == null)
	        		f = dialog.pickFileSave();
	        	
	        	if( f != null) {
	        		saveWorkspace(currentWorkspace, f);
	        		settingsManager.setWorkspaceFilePath(f);
	        	}
        	}
        	break;}
    	case "save_image_as": {
			File f = dialog.pickFileSave();
			
			if( f != null) {
				saveWorkspace(currentWorkspace, f);
			}
    		break;}
    	case "new_image":
    		dialog.promptNewImage();
        	break;
    	case "debug_color":
    		dialog.promptDebugColor();
    		break;
    	case "open_image": {
			File f =dialog.pickFileOpen();
			
			if( f != null) {
	        	loadEngine.openFile( f);
			}
			break;}
    	case "export":
    	case "export_as":{
			File f = dialog.pickFileExport();
			
			if( f != null) {
				exportWorkspaceToFile( currentWorkspace, f);
			}
			break;}
    		
       	default:
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown global command: global." + command);
    	}
    }
    
    private void exportWorkspaceToFile( ImageWorkspace workspace, File f) {
    	String ext = f.getName().substring( f.getName().lastIndexOf(".")+1);
    	
    	RenderSettings settings = new RenderSettings();
    	settings.workspace = workspace;
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
	public void structureChanged(StructureChange evt) {
		triggerImageStructureRefresh();
	}

	@Override
	public void imageChanged( ImageChangeEvent evt) {
		triggerImageRefresh();
	}


}
