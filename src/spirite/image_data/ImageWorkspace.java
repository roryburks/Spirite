package spirite.image_data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageWorkspace {
	private List<Part> parts;
	private List<Rig> rigs;
	private List<Scene> scenes;
	private GroupTree groups;
	
	private GroupTree.Node selected = null;
	
	private int width = 0;
	private int height = 0;
	
	SpiriteImage image;
	
	
	public ImageWorkspace() {
		parts = new ArrayList<Part>();http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1336
		rigs = new ArrayList<Rig>();
		scenes = new ArrayList<Scene>();
		
		groups = new GroupTree();
		
		image = null;
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
	
	// Creates a New Rig
	public Rig newRig( int w, int h, String name, Color c) {
		Rig rig = new Rig(w, h, name, c);		
		groups.addContextual(null, rig);
		rigs.add(rig);
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		return rig;
	}
	
	public Part getActivePart() {
		Rig rig = rigs.get(0);
		if( rig == null) return null;
		
		return rig.debugGetPart();
	}
	
	// Creates a queue of images for drawing purposes
	public List<BufferedImage> getDrawingQueue() {
		List<BufferedImage> queue = new ArrayList<BufferedImage>();
		
		_gdq_rec( groups.getRoot(), queue);
		
		return queue;
	}
	private void _gdq_rec( GroupTree.Node node, List<BufferedImage>queue) {
		for( GroupTree.Node child : node.getChildren()) {
			if( child.isVisible()) {
				if( child instanceof GroupTree.RigNode) {
					// !!!! Very Debug [TODO]
					((GroupTree.RigNode)child).getRig().debugGetPart().getData();
				}
				else {
					_gdq_rec( child, queue);
				}
			}
		}
	}
	
/*	public SpiriteImage newImage( int w, int h, Color c) {
		image = new SpiriteImage( w, h, c);
		
		return image;
	}
	
	
	public SpiriteImage getImage() {
		return image;
	}
	public Part getActivePart() {
		if( image == null) return null;
		return image.getActivePart();
	}*/
}
