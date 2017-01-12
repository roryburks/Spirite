package spirite.image_data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;


/***
 * An ImageWorkspace is essentially the root class for all aspects of the graphical
 * data, both storage and interface.
 * 
 * Multiple ImageWorkspaces can exist, though their interactions with each other are
 * minimal.
 * 
 * @author Rory Burks
 *
 */
public class ImageWorkspace {
	// ImageData is tracked in ImageWorkspace if it is either part of the active
	//	Image Data (used by something in the GroupTree) or it used by a component
	//	stored in the UndoEngine.
	List<ImageData> imageData;
	private int workingID = 0;	// an incrementing unique ID per imageData
	
	// The GroupTree is the primary container for all tracked ImageData.  Though
	//	there can be other logical collections that also have ImageData parts to
	//	it, all data used in the workspace should have an entry in GroupTree even
	//	if it has entries elsewhere
	private GroupTree groupTree;
	private GroupTree.Node selected = null;
	private UndoEngine undoEngine;
	
	private int width = 0;
	private int height = 0;
	
	private String name;
	private boolean changed;
	
	
	public ImageWorkspace() {
		imageData = new ArrayList<ImageData>();
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this);
	}
	
	// :::: Maintenance Methods
	public void cleanDataCache() {
		boolean used[] = new boolean[imageData.size()];
		Arrays.fill(used, false);
		
		// Go through each Layer in the groupTree and flag the imageData
		//	it uses as being used.
		List< LayerNode> layers = groupTree.getAllLayerNodes();
		for( LayerNode node : layers) {
			ImageData data = node.getImageData();
			int i = imageData.indexOf(data);
			
			if( i == -1) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Found untracked ImageData during Cache cleanup.");
				imageData.add(data);
			}
			else 
				used[i] = true;
		}
		
		for( int i=used.length-1; i>=0; --i) {
			if( !used[i]) {
				System.out.println("Clearing: " + i);
				imageData.remove(i);
			}
		}
	}

	
	public void resetUndoEngine() {
		undoEngine.reset();
	}
	
	// :::: Getters and Setters
	public int getWidth() {
		return width;
	}
	public void setWidth( int width) {
		if( width >= 0 && this.width != width
				&& width < Globals.getMetric("workspace.max_size").width) 
		{
			this.width = width;
		}
	}
	public int getHeight() {
		return height;
	}
	public void setHeight( int height) {
		if( height >= 0 && this.height != height
				&& height < Globals.getMetric("workspace.max_size").height)
		{
			this.height = height;
		}
	}
	
	public UndoEngine getUndoEngine() {
		return undoEngine;
	}
	
	public GroupTree.GroupNode getRootNode() {
		return groupTree.getRoot();
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
		
		if( selected != node) {
			selected = node;
			triggerSelectedChanged();
		}
	}
	
	// :::: New Rig Creation
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
		LayerNode node = groupTree.new LayerNode( data, name);
		
		executeChange(createAdditionEvent(node, context));
		
		return node;
	}
	
	public LayerNode addNewRig( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData) {
			if( data.id == identifier) {
				// Create node then execute StructureChange event
				LayerNode node = groupTree.new LayerNode( data, name);

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
		GroupTree.GroupNode newNode = groupTree.new GroupNode(name);
		
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
		if( nodeToMove == null || nodeAbove == null || nodeAbove.getParent() == null 
				|| nodeToMove.getParent() == null || groupTree._isChild( nodeToMove, nodeAbove.getParent()))
			return;
		
		
		executeChange( new MoveChange(
				nodeToMove,
				nodeToMove.getParent(),
				nodeToMove.getNextNode(),
				nodeAbove.getParent(),
				nodeAbove));
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		if( nodeToMove == null || nodeUnder == null || nodeUnder.getParent() == null 
				|| nodeToMove.getParent() == null || groupTree._isChild( nodeToMove, nodeUnder.getParent()))
			return;
		
		
		List<Node> children = nodeUnder.getParent().getChildren();
		int i = children.indexOf(nodeUnder);
		Node nodeBefore;
		
		if( i+1 == children.size())
			nodeBefore = null;
		else
			nodeBefore = children.get(i+1);
		
		executeChange( new MoveChange(
				nodeToMove,
				nodeToMove.getParent(),
				nodeToMove.getNextNode(),
				nodeUnder.getParent(),
				nodeBefore));
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		if( nodeToMove == null || nodeInto == null || nodeInto.getParent()== null 
				|| nodeToMove.getParent() == null || groupTree._isChild( nodeToMove, nodeInto))
			return;
		
		Node nodeBefore = null;
		if( top && !nodeInto.getChildren().isEmpty()) {
			nodeBefore = nodeInto.getChildren().get(0);
		}

		executeChange( new MoveChange(
				nodeToMove,
				nodeToMove.getParent(),
				nodeToMove.getNextNode(),
				nodeInto,
				nodeBefore));
	}
	
	
	// Creates a queue of images for drawing purposes
	public List<LayerNode> getDrawingQueue() {
		List<LayerNode> queue = new LinkedList<LayerNode>();
		
		_gdq_rec( groupTree.getRoot(), queue);
		
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
	
	
	public void renameNode( Node node, String newName) {
		if( newName != node.name)
			executeChange( new RenameChange(newName, node));
	}
	
	public void setNodeVisibility( Node node, boolean visible) {
		if( node.isVisible() != visible) {
			executeChange( new VisbilityChange(node, visible));
		}
	}

	
	/***
	 * A StructureChange class defines all sorts of GroupTree structure changes
	 *
	 */
	public abstract class StructureChange {
		public String description = "Unknown Structure Change";
		boolean imageChange = true;
		public abstract void execute();
		public abstract void unexecute();
		
		// Called when history has been re-written and so the action never
		//	was performed, and thus 
		public void cauterize() {}
		public void alert( boolean undo) {
			triggerStructureChanged(this, undo);
			if( imageChange)
				refreshImage();
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
			description = "Added Node";
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
			description = "Deleted Node";
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
	}
	
	public class RenameChange extends StructureChange {
		public final String newName;
		public final String oldName;
		public final Node node;	// if null, then it's renaming the workspace
		
		public RenameChange(
				String newName,
				Node node) 
		{
			description = "Renamed Node";
			imageChange = false;
			this.newName = newName;
			this.oldName = node.name;
			this.node = node;
		}

		@Override
		public void execute() {
			node.setName(newName);
		}
		@Override
		public void unexecute() {
			node.setName(oldName);
		}
	}
	
	public class MoveChange extends StructureChange {
		public final Node moveNode;
		public final Node oldParent;
		public final Node newParent;
		public final Node oldNext;
		public final Node newNext;
		
		
		public MoveChange(
				Node moveNode,
				Node oldParent,
				Node oldNext,
				Node newParent,
				Node newNext) 
		{
			description = "Moved Node";
			this.moveNode = moveNode;
			this.oldParent = oldParent;
			this.oldNext = oldNext;
			this.newParent = newParent;
			this.newNext = newNext;
		}

		@Override
		public void execute() {
			moveNode._del();
			newParent._add(moveNode, newNext);
		}

		@Override
		public void unexecute() {
			moveNode._del();
			oldParent._add(moveNode, oldNext);
		}
		
	}
	public class VisbilityChange extends StructureChange {
		public final boolean visibleAfter;
		public final Node node;
		
		VisbilityChange( Node node, boolean visible) {
			this.node = node;
			this.visibleAfter = visible;
			this.description = "Visibility Change";
		}

		@Override
		public void execute() {
			node.setVisible(visibleAfter);
		}

		@Override
		public void unexecute() {
			node.setVisible(!visibleAfter);
		}
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
				node.getNextNode());
	}
	
	public StructureChange createAdditionEvent( Node newNode, Node context ) {
		Node parent;
		Node nodeBefore;
		if( context == null) {
			parent = groupTree.getRoot();
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
			nodeBefore = context.getNextNode();
		}

		return new AdditionChange( 
				newNode,
				parent,
				nodeBefore);
	}

	
	
	/***
	 * Verifies that the given node exists within the current workspace
	 */
	public boolean nodeInWorkspace( GroupTree.Node node) {
		List<Node> nodes = groupTree.getAllNodes();
		
		return nodes.contains(node);
	}
	
	// :::: Observers
    List<MImageObserver> imageObservers = new ArrayList<>();
    
    void triggerStructureChanged( StructureChange evt, boolean undo) {
        for( MImageObserver obs : imageObservers) {
            obs.structureChanged( evt);
        }
    }
    public void refreshImage() {
        for( MImageObserver obs : imageObservers) {
            obs.imageChanged();
        }
    }

    public void addImageObserver( MImageObserver obs) { imageObservers.add(obs);}
    public void removeImageObserver( MImageObserver obs) { imageObservers.remove(obs); }
    
    public static interface MImageObserver {
        public void imageChanged();
        public void structureChanged(StructureChange evt);
    }
    

    List<MSelectionObserver> selectionObservers = new ArrayList<>();
    
    public void triggerSelectedChanged() {
        for( MSelectionObserver obs : selectionObservers) {
            obs.selectionChanged( selected);
        }
    }

    public void addSelectionObserver( MSelectionObserver obs) { selectionObservers.add(obs);}
    public void removeSelectionObserver( MSelectionObserver obs) { selectionObservers.remove(obs); }
    
    public static interface MSelectionObserver{
    	public void selectionChanged( Node newSelection);
    }
}