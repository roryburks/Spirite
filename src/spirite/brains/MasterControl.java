package spirite.brains;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import spirite.draw_engine.DrawEngine;
import spirite.draw_engine.RenderEngine;
import spirite.image_data.ImageWorkspace;
import spirite.ui.FrameManager;

/***
 * Master Control is little more than a container for all the various components 
 * which handle the grand internals of the program.
 * 
 * @author Rory Burks
 *
 */
public class MasterControl {
	// Components
    HotkeyManager hotkeys;
    PaletteManager palette;
    ToolsetManager toolset;
    ImageWorkspace image_manager;
    DrawEngine draw_engine;
    RenderEngine render_engine;
    
    FrameManager frame_manager;
    
    Color current_color = new Color(120,160,160);
    
    int width = 0;
    int height = 0;
    int image_update = 0;
    

    public MasterControl() {
        hotkeys = new HotkeyManager();
        palette = new PaletteManager();
        toolset = new ToolsetManager();
        image_manager = new ImageWorkspace();
        frame_manager = new FrameManager( this);
        draw_engine = new DrawEngine( this);
        render_engine = new RenderEngine( this);
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
    	return image_manager;
    }
    public FrameManager getFrameManager() {
    	return frame_manager;
    }
    public DrawEngine getDrawEngine() {
    	return draw_engine;
    }
    public RenderEngine getRenderEngine(){
    	return render_engine;
    }
    

    // !!!! TODO DEBUG
    public void setCurrentWorkpace( ImageWorkspace workspace) {

    	if( workspace != null) {
    		this.image_manager = workspace;
    		this.currentWorkspaceChanged();
    	}
    }
    

    // ==== Image Managements
    public void newImage( int width, int height) {newImage(width,height,new Color(0,0,0,0));}
    public void newImage( int width, int height, Color color) {
//    	image_manager.newImage(width, height, color);
    	image_manager.newRig(width, height, "shamma", color);

        for( MImageObserver obs : imageObservers) {
            obs.newImage();
        }
    }

    public void refreshImage() {
        image_update++;
        for( MImageObserver obs : imageObservers) {
            obs.imageChanged();
        }
    }

    public int getImageUpdate() {
        return image_update;
    }

    public int getWidth() { return width;}
    public int getHeight() { return height;}
    public int getDefaultWidth() { return 128;}
    public int getDefaultHeight() { return 128;}

    

    // ==== Observer Interfaces ====
    List<MImageObserver> imageObservers = new ArrayList<>();

    public void addImageObserver( MImageObserver obs) { imageObservers.add(obs);}
    public void removeImageObserver( MImageObserver obs) { imageObservers.remove(obs); }
    
    public static interface MImageObserver {
        public void imageChanged();
        public void newImage();
    }
    
    

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
    
}
