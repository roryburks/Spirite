package spirite.brains;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    final HotkeyManager hotkeys;
    final PaletteManager palette;
    final ToolsetManager toolset;
    final SettingsManager settingsManager;
    final RenderEngine renderEngine;
    final CacheManager cacheManager;
    

    final FrameManager frame_manager;

    final List<ImageWorkspace> workspaces = new ArrayList<>();
    ImageWorkspace currentWorkspace = null;
    

    public MasterControl() {
        hotkeys = new HotkeyManager();
        palette = new PaletteManager();
        toolset = new ToolsetManager();
        settingsManager = new SettingsManager();
        cacheManager = new CacheManager();
        frame_manager = new FrameManager( this);
        renderEngine = new RenderEngine( this);

        Dialogs.setMaster(this); //// TODO BAD
        LoadEngine.setMaster(this); //// TODO BAD
    }


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
    public FrameManager getFrameManager() {
    	return frame_manager;
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
    
    // :::: API
    
    public void closeWorkspace( ImageWorkspace workspace) {
    	int i = workspaces.indexOf(workspace);
    	
    	if( i == -1) {
    		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to remove a workspace that is not being tracked.");
    		return;
    	}
    	
    	if( workspace.hasChanged()) {
    		// Prompt the User to Save the file before closing if it's 
    		//	changed and respond accordingly.
	    	int ret = JOptionPane.showConfirmDialog(
	    			null,
	    			"Save file before closing?",
	    			"Closing " + workspace.getFileName(),
	    			JOptionPane.YES_NO_CANCEL_OPTION,
	    			JOptionPane.QUESTION_MESSAGE
	    			);
	    	
	    	if( ret == JOptionPane.CANCEL_OPTION)
	    		return;
	    	
	    	if( ret == JOptionPane.YES_OPTION) {
	    		File f = workspace.getFile();
	    		
	    		if( f == null)
	    			f = Dialogs.pickFileSave();
	    		
	    		if( f != null)
	    			SaveEngine.saveWorkspace(workspace, workspace.getFile());
	    		else
	    			return;
	    	}
    	}
    	
    	// Remove the workspace
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
     */
	public void addWorkpace(ImageWorkspace workspace, boolean select) {
		workspaces.add(workspace);
		triggerNewWorkspace(workspace);
		
		workspace.addImageObserver(this);
		
		if( select || currentWorkspace == null) {
			setCurrentWorkpace(workspace);
		}
	}
    

    // ==== Image Managements
    public void newWorkspace( int width, int height) {newWorkspace(width,height,new Color(0,0,0,0), true);}
    public void newWorkspace( int width, int height, Color color, boolean selectOnCreate) {
    	ImageWorkspace ws = new ImageWorkspace( cacheManager);
    	ws.newLayer(width, height, "Background", color);
    	
    	workspaces.add( ws);
    	ws.addImageObserver( this);
    	
    	triggerNewWorkspace(ws);
    	if( selectOnCreate || currentWorkspace == null) {
    		setCurrentWorkpace( ws);
    	}
    }

    public int getDefaultWidth() { return 128;}
    public int getDefaultHeight() { return 128;}

    

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
