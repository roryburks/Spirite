package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
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
	private List<ImageData> imageData;
	private List<Layer> layers;
	private List<Scene> scenes;
	private GroupTree groups;
	
	private GroupTree.Node selected = null;
	
	private int workingID = 0;
	
	private int width = 0;
	private int height = 0;
	
	
	public ImageWorkspace() {
		imageData = new ArrayList<ImageData>();
		layers = new ArrayList<Layer>();
		scenes = new ArrayList<Scene>();
		
		groups = new GroupTree(this);
		
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public GroupTree.GroupNode getRootNode() {
		return groups.getRoot();
	}
	
	public List<ImageData> getImageData() {
		List<ImageData> list = new ArrayList<>(imageData.size());
		
		for( ImageData data : imageData) {
			list.add( data);
		}
		
		return list;
	}

	// :::: The activePart is the part (i.e. raw image data) which will be drawn on
	//	when the user gives input.
	private int selectedLayer = -1;
	
	public ImageData getActiveData() {
		if( selectedLayer < 0) return null;
		
		Layer rig = layers.get( selectedLayer);
		if( rig == null) return null;
		
		return rig.getActiveData();
	}
	
	public void setActiveLayer( Layer rig) {
		selectedLayer = layers.indexOf(rig);
		System.out.println(selectedLayer);
	}
	
	// Creates a New Rig
	public Layer newRig( int w, int h, String name, Color c) {
		return 	addNewRig( null, w, h, name, c);
	}
	
	public Layer addNewRig(  GroupTree.Node context, int w, int h, String name, Color c) {

		Layer rig = new SimpleLayer(w, h, name, c);		
		groups.addContextual(context, rig);
		layers.add(rig);
		
		//!!!! TODO : Reimagine a better way to link Image Data to Workspace
		imageData.add(rig.getActiveData());
		rig.getActiveData().id = workingID++;	// PostIncrement
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		setActiveLayer(rig);
		
		
		alertStructureChanged();
		return rig;
	}
	
	public Layer addNewRig( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData) {
			if( data.id == identifier) {
				Layer rig = new SimpleLayer( data, name);
				groups.addContextual(context, rig);
				
				width = Math.max(width, data.getData().getWidth());
				height = Math.max(height, data.getData().getHeight());
				
				layers.add(rig);
				setActiveLayer(rig);
				
				alertStructureChanged();
				return rig;
			}
		}
		
		System.out.println("Bad");
		
		return null;
	}
	
	public GroupTree.GroupNode addTreeNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groups.addContextual(context, name);
		alertStructureChanged();
		return newNode;
	}
	

	
	/***
	 * Used Primarily for Loading, this will merely add the imageData to the 
	 * Workspace's data set, but it will not be linked logically in any way
	 * (for that you'll need to add a Node to the GroupTree
	 */
	public void addImageDataDirect( ImageData newData) {
		imageData.add( newData);
	}
	
	
	// :::: Move Nodes
	public void moveAbove( Node nodeToMove, Node nodeAbove) {
		groups.moveAbove(nodeToMove, nodeAbove);
		alertStructureChanged();
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		groups.moveBelow(nodeToMove, nodeUnder);
		alertStructureChanged();
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		groups.moveInto(nodeToMove, nodeInto, top);
		alertStructureChanged();
	}
	
	
	// Creates a queue of images for drawing purposes
	public List<Layer> getDrawingQueue() {
		List<Layer> queue = new LinkedList<Layer>();
		
		_gdq_rec( groups.getRoot(), queue);
		
		return queue;
	}
	private void _gdq_rec( GroupTree.Node node, List<Layer>queue) {
		for( GroupTree.Node child : node.getChildren()) {
			if( child.isVisible()) {
				if( child instanceof GroupTree.LayerNode) {
					// !!!! Very Debug [TODO]
					queue.add(0,((GroupTree.LayerNode)child).getLayer());
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
