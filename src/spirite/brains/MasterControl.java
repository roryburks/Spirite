package spirite.brains;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import spirite.dialogs.Dialogs;
import spirite.draw_engine.DrawEngine;
import spirite.draw_engine.RenderEngine;
import spirite.draw_engine.UndoEngine;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.MImageStructureObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.ui.FrameManager;

/***
 * Master Control is little more than a container for all the various components 
 * which handle the grand internals of the program.
 * 
 * @author Rory Burks
 *
 */
public class MasterControl
	implements MImageObserver, MImageStructureObserver 
{
	// Components
    HotkeyManager hotkeys;
    PaletteManager palette;
    ToolsetManager toolset;
    ImageWorkspace imageManager;
    SettingsManager settingsManager;
    DrawEngine drawEngine;
    RenderEngine renderEngine;
    
    FrameManager frame_manager;
    
    Color current_color = new Color(120,160,160);
    
    int width = 0;
    int height = 0;
    

    public MasterControl() {
        hotkeys = new HotkeyManager();
        palette = new PaletteManager();
        toolset = new ToolsetManager();
        imageManager = new ImageWorkspace();
        settingsManager = new SettingsManager();
        frame_manager = new FrameManager( this);
        drawEngine = new DrawEngine();
        renderEngine = new RenderEngine( this);
        Dialogs.setMaster(this); //// TODO BAD
        
        imageManager.addImageObserver(this);
        imageManager.addImageStructureObserver(this);
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
    	return imageManager;
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

    // !!!! TODO DEBUG
    public void setCurrentWorkpace( ImageWorkspace workspace) {

    	if( workspace != null) {
    		this.imageManager = workspace;
    		this.currentWorkspaceChanged();
    	}
    }
    

    // ==== Image Managements
    public void newImage( int width, int height) {newImage(width,height,new Color(0,0,0,0));}
    public void newImage( int width, int height, Color color) {
//    	image_manager.newImage(width, height, color);
    	imageManager.newRig(width, height, "shamma", color);
    }

    public int getWidth() { return width;}
    public int getHeight() { return height;}
    public int getDefaultWidth() { return 128;}
    public int getDefaultHeight() { return 128;}

    

    // ==== Observer Interfaces ====
    List<MWorkspaceObserver> workspaceObservers = new ArrayList<>();

    public void addWorkspaceObserver( MWorkspaceObserver obs) { workspaceObservers.add(obs);}
    public void removeWorkspaceObserver( MWorkspaceObserver obs) { workspaceObservers.remove(obs); }
    
    private void currentWorkspaceChanged() {
    	for( MWorkspaceObserver obs : workspaceObservers) {
    		obs.currentWorkspaceChanged();
    	}
    }
    private void newWorkspace() {
    	for( MWorkspaceObserver obs : workspaceObservers) {
    		obs.newWorkspace();
    	}
    }
    
    public static interface MWorkspaceObserver {
        public void currentWorkspaceChanged();
        public void newWorkspace();
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

	@Override
	public void structureChanged(StructureChangeEvent evt) {
		triggerImageStructureRefresh();
	}


	@Override
	public void imageChanged() {
		triggerImageRefresh();
		
	}


	@Override
	public void newImage() {
		triggerImageStructureRefresh();
	}
}
