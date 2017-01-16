package spirite.image_data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.brains.CacheManager;
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
	
	// Internal Components
	private final GroupTree groupTree;
	private final UndoEngine undoEngine;
	private final AnimationManager animationManager;
	private final SelectionEngine selectionEngine;
	private final DrawEngine drawEngine;
	
	// External Components
//	private final CacheManager cacheManager;
	
	private GroupTree.Node selected = null;
	private int workingID = 0;	// an incrementing unique ID per imageData
	
	private int width = 0;
	private int height = 0;
	
	private File file = null;
	private boolean changed = false;
	
	
	public ImageWorkspace( CacheManager cacheManager) {
//		this.cacheManager = cacheManager;
		
		imageData = new ArrayList<ImageData>();
		animationManager = new AnimationManager(this);
		selectionEngine = new SelectionEngine(this);
		drawEngine = new DrawEngine(this);
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this, cacheManager);
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

	/** Called when an image is first made or is first loaded, resets the UndoEngine
	 * so that various internal actions regarding constructing the workspace are not
	 * undoable.*/
	public void resetUndoEngine() {
		undoEngine.reset();
		changed = false;
	}
	
	/** Called when an image is saved, remembers the undo position to correctly track
	 * if an image is at its saved state or not. */
	public void fileSaved( File newFile) {
		file = newFile;
		changed = false;
		undoEngine.setSaveSpot();
		
		triggerFileChange();
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
	
	public File getFile() {
		return file;
	} 
	
	public String getFileName() {
		if( file == null) 
			return "Untitled Image";
		else
			return file.getName();
	}
	
	public boolean hasChanged() {
		return changed;
	}
	
	public UndoEngine getUndoEngine() {
		return undoEngine;
	}
	
	public AnimationManager getAnimationManager() {
		return animationManager;
	}
	
	public SelectionEngine getSelectionEngine() {
		return selectionEngine;
	}
	
	public DrawEngine getDrawEngine() {
		return drawEngine;
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
		if( !nodeInWorkspace(selected)) {
			setSelectedNode(null);
		}
		return selected;
	}
	public void setSelectedNode( GroupTree.Node node) {
		if( node != null && !nodeInWorkspace(node)) 
			return;
		
		if( selected != node) {
			selected = node;
			triggerSelectedChanged();
		}
	}
	
	// :::: Image Checkout
	public BufferedImage checkoutImage( ImageData image) {
		if( !verifyImage(image))
			return null;
		
		undoEngine.prepareContext(image);
		
		// !!! TODO: Strict image locking and unlocking seems like too much trouble.
		//	BufferedImages are buffered so you don't have to worry too much about
		//	that, but all the same I should have some way to keep track of what
		//	objects have images checked out so that they are checked in eventually
		//	and that they terminate correctly if the image were to be unloaded.
		
		// Violates the spirit of readImage, but changing image.data's visibility
		//	to Package just for this would defeat the entire purpose.
		return image.readImage().image;	
	}
	
	public void checkinImage( ImageData image) {
		if( !verifyImage(image))
			return;

		ImageChangeEvent evt = new ImageChangeEvent();
		evt.dataChanged.add(image);
		evt.workspace = this;
		
		triggerImageRefresh( evt);
	}
	
	private boolean verifyImage( ImageData image) {
		if( !imageData.contains(image)) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to checkout/in image from wrong workspce.");
			return false;
		}
		return true;
	}
	
	// :::: Group Node Modification
	public LayerNode newLayer( int w, int h, String name, Color c) {
		return 	addNewLayer( null, w, h, name, c);
	}
	
	public LayerNode addNewLayer(  GroupTree.Node context, int w, int h, String name, Color c) {
		// Create new Image Data and link it to the workspace
		ImageData data = new ImageData(w, h, c, this);
		imageData.add(data);
		data.id = workingID++;
		
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		// Create node then execute StructureChange event
		LayerNode node = groupTree.new LayerNode( data, name);
		
		executeChange(createAdditionEvent(node, context));
		
		return node;
	}
	
	public LayerNode addNewLayer( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData) {
			if( data.id == identifier) {
				// Create node then execute StructureChange event
				LayerNode node = groupTree.new LayerNode( data, name);

				width = Math.max(width,  data.readImage().getWidth());
				height = Math.max(height, data.readImage().getHeight());
				
				executeChange(createAdditionEvent(node, context));
				
				return node;
			}
		}
		
		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to add a new Simple Layer using an identifier that does not exist in the current workspace.");
		
		return null;
	}
	
	public GroupTree.GroupNode addGroupNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groupTree.new GroupNode(name);
		
		executeChange(createAdditionEvent(newNode,context));
		
		return newNode;
	}
	
	/***
	 * Used Primarily for Loading, this will merely add the imageData to the 
	 * Workspace's data set, but it will not be linked logically in any way
	 * (for that you'll need to add a Node to the GroupTree otherwise it
	 * will get erased next time cleanDataCache is called)
	 */
	public void addImageDataDirect( ImageData newData) {
		imageData.add( newData);
		if( newData.id < workingID)
			newData.id = workingID++;
		else
			workingID = newData.id+1;
	}
	
	// :::: Remove Nodes
	public void removeNode( Node node) {
		if(!nodeInWorkspace(node) ) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Attempted to remove a node from the wrong Workspace.");
			return;
		}
		if( node == groupTree.getRoot()){
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Attempted to remove a Root Node.");
			return;
		}
		
		executeChange( createDeletionEvent(node));
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
	
	
	// :::: Node Attribute Changes
	public void renameNode( Node node, String newName) {
		if( newName != node.name)
			executeChange( new RenameChange(newName, node));
	}
	
	public void setNodeVisibility( Node node, boolean visible) {
		if( nodeInWorkspace(node) && node.isVisible() != visible) {
			executeChange( new VisibilityChange(node, visible));
		}
	}
	public void setNodeAlpha( Node node, float alpha) {
		if( nodeInWorkspace(node) && node.alpha != alpha) {
			executeChange( new OpacityChange(node, alpha));
		}
	}

	
	/**
	 * A StructureChange class logically represent a change to the image's Group
	 * Structure.  The classes implement both performing that change and undoing
	 * that change (as such it must remember the information required for doing that).
	 * these methods are called both for the initial execution and from the UndoEngine
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
			if( imageChange) {
				ImageChangeEvent evt = new ImageChangeEvent();
				evt.workspace = ImageWorkspace.this;
				triggerImageRefresh( evt);
			}
		}
	}
	
	/** A StackableStructureChange corresponds to a StackableAction (see 
	 * UndoEngine.StackableAction), meaning that performing the action over
	 * and over on the same node should consolidate into a single action. */
	public abstract class StackableStructureChange extends StructureChange {
		public abstract void stackNewChange( StructureChange newChange);
		public abstract boolean canStack( StructureChange newChange);
	}
	
	/** Adding a node to the tree. */
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
	
	/** Removing a Node from the tree */
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
	
	/** Moving the node from one place to another. */
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
	
	/** Renaming the node */
	public class RenameChange extends StructureChange {
		public final String newName;
		public final String oldName;
		public final Node node;
		
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
	
	/** Changing the node's visibility */
	public class VisibilityChange extends StructureChange {
		public final boolean visibleAfter;
		public final Node node;
		
		VisibilityChange( Node node, boolean visible) {
			this.node = node;
			this.visibleAfter = visible;
			
			this.description = "Visibility Changed";
		}

		@Override
		public void execute() {
			node.visible = visibleAfter;
		}

		@Override
		public void unexecute() {
			node.visible = !visibleAfter;
		}
	}
	
	/** Changing the node's Opacity */
	public class OpacityChange extends StackableStructureChange
	{
		float opacityBefore;
		float opacityAfter;
		public final Node node;
		
		OpacityChange( Node node, float opacity) {
			this.node = node;
			this.opacityBefore = node.alpha;
			this.opacityAfter = opacity;
			
			this.description = "Opacity Changed";
		}

		@Override
		public void execute() {
			node.alpha = opacityAfter;
		}

		@Override
		public void unexecute() {
			node.alpha = opacityBefore;
		}

		@Override
		public void stackNewChange(StructureChange newChange) {
			OpacityChange change = (OpacityChange)newChange;
			
			this.opacityAfter = change.opacityAfter;
		}
		
		@Override
		public boolean canStack(StructureChange newChange) {
			OpacityChange change = (OpacityChange)newChange;
			
			return (node == change.node);
		}
	}
	
	/** Executes the given change and stores it in the UndoEngine */
	public void executeChange( StructureChange change) {
		change.execute();
		
		if( change instanceof StackableStructureChange) 
			undoEngine.storeAction(undoEngine.new StackableStructureAction(change) , null);
		else 
			undoEngine.storeAction(undoEngine.new StructureAction(change) , null);
		change.alert(false);
	}
	
	// :::: StructureChangeEvent Creation Methods for simplifying the process
	public StructureChange createDeletionEvent( GroupTree.Node node) {
		if( node == this.getRootNode()) 
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

	
	// :::: Utility Methods
	/***
	 * Verifies that the given node exists within the current workspace
	 */
	public boolean nodeInWorkspace( GroupTree.Node node) {
		List<Node> nodes = groupTree.getAllNodes();
		
		return nodes.contains(node);
	}
	

	/** Creates a queue of images for drawing purposes. */
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
	
	// :::: Observers
	/**
	 * ImageObserver - triggers when an image's data has been changed
	 *  (<code>imageChanged</code>) or when the image's logical structure
	 *  has changed (<code>structureChanged</code>)
	 */
    public static interface MImageObserver {
        public void imageChanged(ImageChangeEvent evt);
        public void structureChanged(StructureChange evt);
    }
    public static class ImageChangeEvent {
    	ImageWorkspace workspace = null;
    	LinkedList<ImageData> dataChanged = new LinkedList<>();
    	LinkedList<Node> nodesChanged = new LinkedList<>();
    	boolean selectionLayerChange = false;
    	boolean isUndoEngineEvent = false;	// Probably a more generic way to do this
    	boolean isStructureChange = false;
    	
    	public ImageWorkspace getWorkspace() { return workspace;}
    	public List<ImageData> getChangedImages() { return (List<ImageData>) dataChanged.clone();}
    	public List<Node> getChangedNodes() { return (List<Node>) nodesChanged.clone();}
    	public boolean getSelectionLayerChange() { return selectionLayerChange;}
    	public boolean isStructureChange() {return isStructureChange;}
    }
    List<MImageObserver> imageObservers = new ArrayList<>();
    
    private void triggerStructureChanged( StructureChange evt, boolean undo) {
        for( MImageObserver obs : imageObservers)
            obs.structureChanged( evt);
    }
    void triggerImageRefresh(ImageChangeEvent evt) {
        for( MImageObserver obs : imageObservers)
            obs.imageChanged(evt);
        
        if( evt.isUndoEngineEvent && undoEngine.atSaveSpot()) {
			changed = false;
			triggerFileChange();
        }
		else if( !changed) {
			changed = true;
			triggerFileChange();
		}
    }

    public void addImageObserver( MImageObserver obs) { imageObservers.add(obs);}
    public void removeImageObserver( MImageObserver obs) { imageObservers.remove(obs); }
    


    /**
     * SelectionObserver - triggers when a different Layer has been selected
     */
    public static interface MSelectionObserver{
    	public void selectionChanged( Node newSelection);
    }
    List<MSelectionObserver> selectionObservers = new ArrayList<>();
    
    private void triggerSelectedChanged() {
        for( MSelectionObserver obs : selectionObservers)
            obs.selectionChanged( selected);
    }

    public void addSelectionObserver( MSelectionObserver obs) { selectionObservers.add(obs);}
    public void removeSelectionObserver( MSelectionObserver obs) { selectionObservers.remove(obs); }
    
    
    
    /**
     * WorkspaceFileObserver - triggers when the File accompanying the
     * 	workspace has changed or if the status of whether or not the
     *  file is different from its saved form has changed
     */
    public static interface MWorkspaceFileObserver{
    	public void fileChanged( FileChangeEvent evt);
    }
    public static class FileChangeEvent {
    	final ImageWorkspace workspace;
    	final File file;
    	final boolean changed;
    	
    	FileChangeEvent(ImageWorkspace workspace, File newFile, boolean changed) {
    		this.workspace = workspace;
    		this.file = newFile;
    		this.changed = changed;
    	}
    	public ImageWorkspace getWorkspace() {
    		return workspace;
    	}
    	public File getFile() {
    		return file;
    	}
    	public boolean hasChanged() {
    		return changed;
    	}
    }
    List<MWorkspaceFileObserver> fileObservers = new ArrayList<>();
    
    private void triggerFileChange() {
        for( MWorkspaceFileObserver obs : fileObservers)
            obs.fileChanged( new FileChangeEvent( this, file, changed));
    }

    public void addWorkspaceFileObserve( MWorkspaceFileObserver obs) { fileObservers.add(obs);}
    public void removeWorkspaceFileObserve( MWorkspaceFileObserver obs) { fileObservers.remove(obs); }
    
}