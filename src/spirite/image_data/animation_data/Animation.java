package spirite.image_data.animation_data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import spirite.MUtil;
import spirite.image_data.Layer;

public class Animation {
	private ArrayList<Scene> scenes = new ArrayList<>();
	
	private boolean fixedFrame;	// if true, uses only Integers for keyframes
	private float startFrame;
	private float endFrame;
	
	public float getStartFrame() {
		return 0;
	}
	public float getEndFrame() {
		return 0;
	}
	
	public BufferedImage renderFrame( float t) {
		float rt = MUtil.cycle(startFrame, endFrame, t);
		
		
		return null;
	}
	
	// :::: Settings API
	public boolean getFixedFrame() {
		return fixedFrame;
	}
	public void setFixedFrame( ) {
		
	}
	
	// :::: Scene API
	public void addScene() {
		
	}
	public List<Scene> getScenes() {
		return (List<Scene>) scenes.clone();
	}
	public void removeScene( Scene toRemove) {
		
	}
	
	
	//!!!! TODO: Simplicity for now
	public static class Scene {
		private List<Layer> layers = new ArrayList();
		
		
	}
}
