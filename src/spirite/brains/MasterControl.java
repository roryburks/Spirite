package spirite.brains;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.dialogs.Dialogs;
import spirite.image_data.DrawEngine;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.ui.FrameManager;

/***
 * Master Control is little more than a container for all the various components 
 * which handle the grand internals of the program.  
 * 
 * Note: Though most UI components will need full access to MasterControl, giving
 * it to too many internal components is probably indicative of backwards design.
 * 
 * @author Rory Burks
 *
 */
public class MasterControl
	implements MImageObserver 
{
	// Components
    HotkeyManager hotkeys;
    PaletteManager palette;
    ToolsetManager toolset;
    SettingsManager settingsManager;
    DrawEngine drawEngine;
    RenderEngine renderEngine;
    
    FrameManager frame_manager;
    

    List<ImageWorkspace> workspaces = new ArrayList<>();
    ImageWorkspace currentWorkspace = null;
    
    Color current_color = new Color(120,160,160);
    
    int width = 0;
    int height = 0;
    

    public MasterControl() {
        hotkeys = new HotkeyManager();
        palette = new PaletteManager();
        toolset = new ToolsetManager();
        settingsManager = new SettingsManager();
        frame_manager = new FrameManager( this);
        drawEngine = new DrawEngine();
        renderEngine = new RenderEngine( this);
        Dialogs.setMaster(this); //// TODO BAD
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
    public DrawEngine getDrawEngine() {
    	return drawEngine;
    }
    public RenderEngine getRenderEngine(){
    	return renderEngine;
    }
    public SettingsManager getSettingsManager() {
    	return settingsManager;
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
    	
    	if( !workspaces.contains(workspace)) {
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
    	ImageWorkspace ws = new ImageWorkspace();
    	ws.newLayer(width, height, "Background", color);
    	
    	workspaces.add( ws);
    	ws.addImageObserver( this);
    	
    	triggerNewWorkspace(ws);
    	if( selectOnCreate || currentWorkspace == null) {
    		setCurrentWorkpace( ws);
    	}
    }

    public int getWidth() { return width;}
    public int getHeight() { return height;}
    public int getDefaultWidth() { return 128;}
    public int getDefaultHeight() { return 128;}

    

    // ==== Observer Interfaces ====
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
    
    // :::: MCurrentImageObserver
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

	// :::: MImageObserver
	@Override
	public void structureChanged(StructureChange evt) {
		triggerImageStructureRefresh();
	}

	@Override
	public void imageChanged() {
		triggerImageRefresh();
	}


}
