package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.StructureChangeEvent.ChangeType;


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

	
	public ImageData getActiveData() {
		if( selected == null) return null;
		
		if( selected instanceof GroupTree.LayerNode) {
			// !!!! SHOULD be no reason to add sanity checks here.
			return  ((GroupTree.LayerNode)selected).getLayer().getActiveData();
		}
		return null;
	}
	
//	public void setActiveLayer( Layer rig) {
	//	selectedLayer = layers.indexOf(rig);
	//}
	
	public GroupTree.Node getSelectedNode() {
		return selected;
	}
	public void setSelectedNode( GroupTree.Node node) {
		// TODO : Add Sanity check to make sure the node is in the workspace
		
		selected = node;
	}
	
	// Creates a New Rig
	public Layer newRig( int w, int h, String name, Color c) {
		return 	addNewRig( null, w, h, name, c);
	}
	
	public Layer addNewRig(  GroupTree.Node context, int w, int h, String name, Color c) {

		Layer rig = new SimpleLayer(w, h, name, c, this);		
		GroupTree.Node newNode = groups.addContextual(context, rig);
		layers.add(rig);
		
		//!!!! TODO : Reimagine a better way to link Image Data to Workspace
		imageData.add(rig.getActiveData());
		rig.getActiveData().id = workingID++;	// PostIncrement
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		setSelectedNode( newNode);
		

		// Contruct and trigger the StructureChangeEvent
		StructureChangeEvent evt = new StructureChangeEvent(this, ChangeType.ADDITION);
		evt.affectedNodes.add(newNode);
		triggerStructureChanged( evt);
		
		return rig;
	}
	
	public Layer addNewRig( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData) {
			if( data.id == identifier) {
				Layer rig = new SimpleLayer( data, name, this);
				GroupTree.Node newNode = groups.addContextual(context, rig);
				
				width = Math.max(width, data.getData().getWidth());
				height = Math.max(height, data.getData().getHeight());
				
				layers.add(rig);
				setSelectedNode( newNode);
				
				// Contruct and trigger the StructureChangeEvent
				StructureChangeEvent evt = new StructureChangeEvent(this, ChangeType.ADDITION);
				evt.affectedNodes.add(newNode);
				triggerStructureChanged( evt);
				
				return rig;
			}
		}
		
		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to add a new Simple Layer using an identifier that does not exist in the current workspace.");
		
		return null;
	}
	
	public GroupTree.GroupNode addTreeNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groups.addContextual(context, name);

		// Contruct and trigger the StructureChangeEvent
		StructureChangeEvent evt = new StructureChangeEvent(this, ChangeType.ADDITION);
		evt.affectedNodes.add(newNode);
		triggerStructureChanged( evt);
		
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
		
		// Contruct and trigger the StructureChangeEvent
		StructureChangeEvent evt = new StructureChangeEvent(this, ChangeType.MOVE);
		evt.affectedNodes.add(nodeToMove);
		triggerStructureChanged( evt);
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		groups.moveBelow(nodeToMove, nodeUnder);
		
		// Contruct and trigger the StructureChangeEvent
		StructureChangeEvent evt = new StructureChangeEvent(this, ChangeType.MOVE);
		evt.affectedNodes.add(nodeToMove);
		triggerStructureChanged( evt);
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		groups.moveInto(nodeToMove, nodeInto, top);
		
		// Contruct and trigger the StructureChangeEvent
		StructureChangeEvent evt = new StructureChangeEvent(this, ChangeType.MOVE);
		evt.affectedNodes.add(nodeToMove);
		triggerStructureChanged( evt);
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
	public static class StructureChangeEvent {
		public final ImageWorkspace workspace;
		public final ChangeType changeType;
		LinkedList<GroupTree.Node> affectedNodes = new LinkedList<>();
		
		public enum ChangeType {
			ADDITION,
			DELETION,
			RENAME,
			MOVE
		}
		
		StructureChangeEvent( ImageWorkspace workspace, ChangeType changeType) {
			this.workspace = workspace;
			this.changeType = changeType;
		}
		
		public List<GroupTree.Node> getAffectedNode() {
			return (List<Node>) affectedNodes.clone();
		}
	}
	
    private List<MImageStructureObserver> imageStructureObservers = new ArrayList<>();

    public void addImageStructureObserver( MImageStructureObserver obs) { imageStructureObservers.add(obs);}
    public void removeImageStructureeObserver( MImageStructureObserver obs) { imageStructureObservers.remove(obs); }
    
    void triggerStructureChanged( StructureChangeEvent evt) {
        for( MImageStructureObserver obs : imageStructureObservers) {
            obs.structureChanged( evt);
        }
    }
    
    public static interface MImageStructureObserver {
        public void structureChanged(StructureChangeEvent evt);
    }
}