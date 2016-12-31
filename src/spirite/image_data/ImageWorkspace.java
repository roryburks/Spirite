package spirite.image_data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;


/***
 * An ImageWorkspace is 
 * 
 * @author Rory Burks
 *
 */
public class ImageWorkspace {
	private List<Part> parts;
	private List<Rig> rigs;
	private List<Scene> scenes;
	private GroupTree groups;
	
	private GroupTree.Node selected = null;
	
	private int width = 0;
	private int height = 0;
	
	
	public ImageWorkspace() {
		parts = new ArrayList<Part>();
		rigs = new ArrayList<Rig>();
		scenes = new ArrayList<Scene>();
		
		groups = new GroupTree(this);
		
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
		return 	addNewRig( null, w, h, name, c);
	}
	
	public Rig addNewRig(  GroupTree.Node context, int w, int h, String name, Color c) {

		Rig rig = new Rig(w, h, name, c);		
		groups.addContextual(context, rig);
		rigs.add(rig);
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		setActivePart(rig);
		
		
		alertStructureChanged();
		return rig;
	}
	public void addTreeNode( GroupTree.Node context, String name) {
		groups.addContextual(context, name);
		alertStructureChanged();
	}
	

	public void moveAbove( Node nodeToMove, Node nodeAbove) {
		groups.moveAbove(nodeToMove, nodeAbove);
		alertStructureChanged();
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		groups.moveBelow(nodeToMove, nodeUnder);
		alertStructureChanged();
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto) {
		groups.moveInto(nodeToMove, nodeInto);
		alertStructureChanged();
	}
	public void moveIntoTop(Node nodeToMove, GroupNode nodeInto) {
		groups.moveIntoTop(nodeToMove, nodeInto);
		alertStructureChanged();
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
	
	// :::: Observers
    private List<MImageStructureObserver> imageStructureObservers = new ArrayList<>();

    public void addImageStructureObserver( MImageStructureObserver obs) { imageStructureObservers.add(obs);}
    public void removeImageStructureeObserver( MImageStructureObserver obs) { imageStructureObservers.remove(obs); }
    
    void alertStructureChanged() {
        for( MImageStructureObserver obs : imageStructureObservers) {
            obs.structureChanged();
        }
    }
    
    public static interface MImageStructureObserver {
        public void structureChanged();
    }
}
