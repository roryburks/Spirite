package spirite.image_data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.draw_engine.UndoEngine;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;


/***
 * An ImageWorkspace is 
 * 
 * @author Rory Burks
 *
 */
public class ImageWorkspace {
	private List<ImageData> imageData;
	private List<Scene> scenes;
	private GroupTree groups;
	
	private GroupTree.Node selected = null;
	
	private int workingID = 0;
	
	private int width = 0;
	private int height = 0;
	
	private UndoEngine undoEngine;
	
	
	
	public ImageWorkspace() {
		imageData = new ArrayList<ImageData>();
		scenes = new ArrayList<Scene>();
		
		groups = new GroupTree(this);
		
		undoEngine = new UndoEngine(this);
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public UndoEngine getUndoEngine() {
		return undoEngine;
	}
	
	public void resetUndoEngine() {
		undoEngine.reset();
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
			return  ((GroupTree.LayerNode)selected).getImageData();
		}
		return null;
	}
	
	public GroupTree.Node getSelectedNode() {
		return selected;
	}
	public void setSelectedNode( GroupTree.Node node) {
		if( !nodeInWorkspace(node))
			return;
		
		selected = node;
	}
	
	// Creates a New Rig
	public LayerNode newRig( int w, int h, String name, Color c) {
		return 	addNewRig( null, w, h, name, c);
	}
	
	public LayerNode addNewRig(  GroupTree.Node context, int w, int h, String name, Color c) {
		// Create new Image Data and link it to the workspace
		ImageData data = new ImageData(w, h, c, this);
		data.id = workingID++;	// PostIncrement
		imageData.add(data);
		
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		// Create node then execute StructureChange event
		LayerNode node = groups.new LayerNode( data, name);
		
		executeChange(createAdditionEvent(node, context));
		
		return node;
	}
	
	public LayerNode addNewRig( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData) {
			if( data.id == identifier) {
				// Create node then execute StructureChange event
				LayerNode node = groups.new LayerNode( data, name);

				width = Math.max(width,  data.getData().getWidth());
				height = Math.max(height, data.getData().getHeight());
				
				executeChange(createAdditionEvent(node, context));
				
				return node;
			}
		}
		
		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to add a new Simple Layer using an identifier that does not exist in the current workspace.");
		
		return null;
	}
	
	public GroupTree.GroupNode addTreeNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groups.new GroupNode(name);
		
		executeChange(createAdditionEvent(newNode,context));
		
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
		StructureChange evt = null;
//		StructureChange evt = new StructureChange(this, StructureChangeType.MOVE);
//		evt.affectedNodes.add(nodeToMove);
		triggerStructureChanged( evt, false);
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		groups.moveBelow(nodeToMove, nodeUnder);
		
		// Contruct and trigger the StructureChangeEvent
		StructureChange evt = null;
//		StructureChange evt = new StructureChange(this, StructureChangeType.MOVE);
//		evt.affectedNodes.add(nodeToMove);
		triggerStructureChanged( evt,false);
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		groups.moveInto(nodeToMove, nodeInto, top);
		
		// Contruct and trigger the StructureChangeEvent
		StructureChange evt = null;
//		StructureChange evt = new StructureChange(this, StructureChangeType.MOVE);
//		evt.affectedNodes.add(nodeToMove);
		triggerStructureChanged( evt, false);
	}
	
	
	// Creates a queue of images for drawing purposes
	public List<LayerNode> getDrawingQueue() {
		List<LayerNode> queue = new LinkedList<LayerNode>();
		
		_gdq_rec( groups.getRoot(), queue);
		
		return queue;
	}
	private void _gdq_rec( GroupTree.Node node, List<LayerNode>queue) {
		for( GroupTree.Node child : node.getChildren()) {
			if( child.isVisible()) {
				if( child instanceof GroupTree.LayerNode) {
					// !!!! Very Debug [TODO]
					queue.add(0,((GroupTree.LayerNode)child));
				}
				else {
					_gdq_rec( child, queue);
				}
			}
		}
	}
	

	
	/***
	 * A StructureChange class defines all sorts of GroupTree structure changes
	 *
	 */
	public abstract class StructureChange {
		public abstract void execute();
		public abstract void unexecute();
		public abstract void cauterize();
		public void alert( boolean undo) {
			triggerStructureChanged(this, undo);
		}
	}
	
	public class AdditionChange extends StructureChange {
		public final GroupTree.Node node;
		public final GroupTree.Node parent;
		public final GroupTree.Node nodeBefore;
		
		AdditionChange(
				GroupTree.Node node,
				GroupTree.Node parent,
				GroupTree.Node before) 
		{
			this.node = node;
			this.parent = parent;
			this.nodeBefore = before;
		}
		
		@Override
		public void execute() {
			parent._add( node, nodeBefore);
			selected = node;
		}
		@Override
		public void unexecute() {
			node._del();
			if( selected == node)
				selected = null;
		}

		@Override
		public void cauterize() {}
	}
	

	public class DeletionChange extends StructureChange {
		public final GroupTree.Node node;
		public final GroupTree.Node parent;
		public final GroupTree.Node nodeBefore;
		
		DeletionChange(
				GroupTree.Node node,
				GroupTree.Node parent,
				GroupTree.Node before) 
		{
			this.node = node;
			this.parent = parent;
			this.nodeBefore = before;
		}

		@Override
		public void execute() {
			node._del();
		}

		@Override
		public void unexecute() {
			parent._add(node, nodeBefore);
		}

		@Override
		public void cauterize() {}
		
	}
	
	public void executeChange( StructureChange change) {
		change.execute();
		
		undoEngine.storeAction(undoEngine.new StructureAction(change) , null);
		change.alert(false);
	}
	
	// :::: StructureChangeEvent Factories
	public StructureChange createDeletionEvent( GroupTree.Node node) {
		if( node.getParent() == this.getRootNode()) 
			return null;
		else if( !nodeInWorkspace(node)) 
			return null;
		
		return new DeletionChange( 
				node,
				node.getParent(),
				node.getNodeBefore());
	}
	
	public StructureChange createAdditionEvent( Node newNode, Node context ) {
		Node parent;
		Node nodeBefore;
		if( context == null) {
			parent = groups.getRoot();
			nodeBefore = null;
		}
		else if( context instanceof GroupNode) {
			parent = context;
			nodeBefore = null;
		}
		else {
			if( context.getParent() == null)
				return null;
			parent = context.getParent();
			nodeBefore = context;
		}

		return new AdditionChange( 
				newNode,
				parent,
				nodeBefore);
	}

	public StructureChange createMoveEventAbove(Node nodeToMove, Node nodeAbove ) {
		return null;
	}
	public StructureChange createMoveEventBellow(Node nodeToMove, Node nodeAbove ) {
		return null;
	}
	public StructureChange createMoveEventInto(Node nodeToMove, Node nodeAbove, boolean top) {
		return null;
	}
	
	/***
	 * Verifies that the given node exists within the current workspace
	 */
	public boolean nodeInWorkspace( GroupTree.Node node) {
		return _niw_rec( groups.getRoot(), node);
	}
	private boolean _niw_rec( GroupTree.Node working, GroupTree.Node toCheck) {
		if( working == toCheck) 
			return true;
		
		for( GroupTree.Node child : working.getChildren()) {
			return _niw_rec( child, toCheck);
		}
		return false;
	}
	
	// :::: Observers
    private List<MImageStructureObserver> imageStructureObservers = new ArrayList<>();

    public void addImageStructureObserver( MImageStructureObserver obs) { imageStructureObservers.add(obs);}
    public void removeImageStructureeObserver( MImageStructureObserver obs) { imageStructureObservers.remove(obs); }
    
    void triggerStructureChanged( StructureChange evt, boolean undo) {
        for( MImageStructureObserver obs : imageStructureObservers) {
            obs.structureChanged( evt);
        }
    }
    
    public static interface MImageStructureObserver {
        public void structureChanged(StructureChange evt);
    }
    

    List<MImageObserver> imageObservers = new ArrayList<>();
    

    public void refreshImage() {
        for( MImageObserver obs : imageObservers) {
            obs.imageChanged();
        }
    }

    public void addImageObserver( MImageObserver obs) { imageObservers.add(obs);}
    public void removeImageObserver( MImageObserver obs) { imageObservers.remove(obs); }
    
    public static interface MImageObserver {
        public void imageChanged();
        public void newImage();
    }
}