package spirite.image_data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ImageWorkspace {
	private List<Part> parts;
	private List<Rig> rigs;
	private List<Scene> scenes;
	private GroupTree groups;
	
	private GroupTree.Node selected = null;
	
	private int width = 0;
	private int height = 0;
	
	
	public ImageWorkspace() {
		parts = new ArrayList<Part>();http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1336
		rigs = new ArrayList<Rig>();
		scenes = new ArrayList<Scene>();
		
		groups = new GroupTree();
		
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public GroupTree.Node getRootNode() {
		return groups.getRoot();
	}

	// :::: The activePart is the part (i.e. raw image data) which will be drawn on
	//	when the user gives input.
	private int selected_rig = -1;
	
	public Part getActivePart() {
		if( selected_rig < 0) return null;
		
		Rig rig = rigs.get( selected_rig);
		if( rig == null) return null;
		
		return rig.debugGetPart();
	}
	
	public void setActivePart( Rig rig) {
		selected_rig = rigs.indexOf(rig);
	}
	
	// Creates a New Rig
	public Rig newRig( int w, int h, String name, Color c) {
		Rig rig = new Rig(w, h, name, c);		
		groups.addContextual(null, rig);
		rigs.add(rig);
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		setActivePart(rig);
		return rig;
	}
	
	
	// Creates a queue of images for drawing purposes
	public List<BufferedImage> getDrawingQueue() {
		List<BufferedImage> queue = new LinkedList<BufferedImage>();
		
		_gdq_rec( groups.getRoot(), queue);
		
		return queue;
	}
	private void _gdq_rec( GroupTree.Node node, List<BufferedImage>queue) {
		for( GroupTree.Node child : node.getChildren()) {
			if( child.isVisible()) {
				if( child instanceof GroupTree.RigNode) {
					// !!!! Very Debug [TODO]
					queue.add(0,((GroupTree.RigNode)child).getRig().debugGetPart().getData());
				}
				else {
					_gdq_rec( child, queue);
				}
			}
		}
	}
}
