package spirite.image_data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.brains.CacheManager;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.UndoEngine.BadCompositeConstructionExcpetion;
import spirite.image_data.UndoEngine.ClearAction;
import spirite.image_data.UndoEngine.CompositeAction;
import spirite.image_data.UndoEngine.StructureAction;
import spirite.image_data.UndoEngine.UndoAction;
import spirite.image_data.layers.SimpleLayer;


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
	private final CacheManager cacheManager;
	
	// External Components
//	private final CacheManager cacheManager;
	
	private GroupTree.Node selected = null;
	private int workingID = 0;	// an incrementing unique ID per imageData
	
	private int width = 0;
	private int height = 0;
	
	private File file = null;
	private boolean changed = false;
	private boolean building = true;	// While building, no UndoActionsare stored
										// and no cache is cleared
	
	
	public ImageWorkspace( CacheManager cacheManager) {
		this.cacheManager = cacheManager;
		imageData = new ArrayList<ImageData>();
		animationManager = new AnimationManager(this);
		selectionEngine = new SelectionEngine(this);
		drawEngine = new DrawEngine(this);
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this);
	}
	
	@Override
	public String toString() {
		return "ImageWorkspace: " + getFileName();
	}
	
	// :::: Maintenance Methods
	public void cleanDataCache() {
		if( building) return;
		boolean used[] = new boolean[imageData.size()];
		Arrays.fill(used, false);
		
		// Go through each Layer in the groupTree and flag the imageData
		//	it uses as being used.
		List<Node> layers = groupTree.getRoot().getAllNodesST( new NodeValidator() {
			@Override
			public boolean isValid(Node node) {
				return (node instanceof LayerNode);
			}
			@Override
			public boolean checkChildren(Node node) {return true;}
		});
		for( Node node : layers) {
			List<ImageData> layerDataUsed = ((LayerNode)node).getLayer().getUsedImageData();
			
			for( ImageData data : layerDataUsed) {
				int i = imageData.indexOf(data);
				
				if( i == -1) {
					MDebug.handleWarning(WarningType.STRUCTURAL, this, "Found untracked ImageData during Cache cleanup.");
					imageData.add(data);
				}
				else 
					used[i] = true;
			}
		}
		
		// Go through the UndoEngine and flag them as being used
		for( ImageData image : undoEngine.getDataUsed()) {
			int index = imageData.indexOf(image);
			
			if( index == -1) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Found untracked ImageData from UndoEngine during Cache cleanup.");
				imageData.add(image);
			}
			else if( index < used.length) {	// shouldn't be needed, but can be
				used[index] = true;
			}
		}
		
		for( int index=used.length-1; index>=0; --index) {
			if( !used[index]) {
				imageData.get(index).flush();
				imageData.remove(index);
			}
		}
	}

	/** Called when an image is first made or is first loaded, resets the UndoEngine
	 * so that various internal actions regarding constructing the workspace are not
	 * undoable.*/
	public void finishBuilding() {
		undoEngine.reset();
		building = false;
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
	
	public CacheManager getCacheManager() {
		return cacheManager;
	}
	
	public GroupTree.GroupNode getRootNode() {
		return groupTree.getRoot();
	}
	public GroupTree getGroupTree() {
		return groupTree;
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

		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.dataChanged.add(image);
		evt.workspace = this;
		triggerImageRefresh( evt);
	}
	
	private boolean verifyImage( ImageData image) {
		System.out.println(image);
		if( !imageData.contains(image)) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to checkout/in image from wrong workspce.");
			return false;
		}
		return true;
	}
	
	/** Performs a command of context "draw." */
	public void executeDrawCommand( String command) {
		if( command.equals("clearLayer")) {
			ImageData image = getActiveData();
			if( image != null) {
				ClearAction action = undoEngine.new ClearAction();
				action.performImageAction(image);
				undoEngine.storeAction(action, image);
			}
		}
        else {
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Draw command: draw." + command);
        }
	}
	
	// :::: Group Node Modification
	public LayerNode newLayer( int w, int h, String name, Color c) {
		return 	addNewLayer( null, w, h, name, c);
	}
	
	public LayerNode addNewSimpleLayer( GroupTree.Node context, BufferedImage img, String name) {
		ImageData newImage = new ImageData( img, workingID++, this);
		imageData.add(newImage);

		LayerNode node = groupTree.new LayerNode( new SimpleLayer(newImage), name);
		if( width < img.getWidth() || height < img.getHeight()) {
			List<ImageData> data = new ArrayList<>(2);
			List<UndoAction> actions = new ArrayList<>(2);

			actions.add(undoEngine.new StructureAction( createAdditionEvent(node,context)));
			data.add(null);
			actions.add(undoEngine.new StructureAction( new DimensionChange( Math.max(width, img.getWidth()),Math.max(height, img.getHeight()))));
			data.add(null);
			
			try {
				CompositeAction action = undoEngine.new CompositeAction( actions, data);
				executeComposite(action);
				
				// It's important that you add the action to the UndoEngine AFTER
				//	you add the node to the GroupTree by calling executingComposite
				//	otherwise the UndoEngine will cull the imageData before it's linked
				undoEngine.storeAction(action, null);
			} catch (BadCompositeConstructionExcpetion e) {
				e.printStackTrace();
				return null;
			}
		}
		else
			executeChange( createAdditionEvent(node,context));
		return node;
	}
	
	public LayerNode addNewLayer(  GroupTree.Node context, int w, int h, String name, Color c) {
		// Create new Image Data and link it to the workspace
		ImageData data = new ImageData(w, h, c, this);
		imageData.add(data);
		data.id = workingID++;
		
		
		width = Math.max(width, w);
		height = Math.max(height, h);
		
		// Create node then execute StructureChange event
		LayerNode node = groupTree.new LayerNode( new SimpleLayer(data), name);
		
		executeChange(createAdditionEvent(node, context));
		
		return node;
	}
	
	public LayerNode addNewLayer( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData) {
			if( data.id == identifier) {
				// Create node then execute StructureChange event
				LayerNode node = groupTree.new LayerNode( new SimpleLayer(data), name);

				width = Math.max(width,  data.readImage().getWidth());
				height = Math.max(height, data.readImage().getHeight());
				
				executeChange(createAdditionEvent(node, context));
				
				return node;
			}
		}
		
		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to add a new Simple Layer using an identifier that does not exist in the current workspace. :" + identifier);
		
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

	
	/**
	 * A StructureChange class logically represent a change to the image's Group
	 * Structure.  The classes implement both performing that change and undoing
	 * that change (as such it must remember the information required for doing that).
	 * these methods are called both for the initial execution and from the UndoEngine
	 */
	public abstract class StructureChange {
		public String description = "Unknown Structure Change";
		boolean imageChange = true;
		boolean groupTreeChange = false;
		public abstract void execute();
		public abstract void unexecute();
		public List<Node> getChangedNodes() { return new LinkedList<>();}
		public final boolean isGroupTreeChange() {return groupTreeChange;}
		
		// Called when history has been re-written and so the action never
		//	was performed, and thus 
		public void cauterize() {}
		public void alert( boolean undo) {
			triggerGroupStructureChanged(this, undo);
			if( imageChange) {
				ImageChangeEvent evt = new ImageChangeEvent();
				evt.workspace = ImageWorkspace.this;
				evt.nodesChanged.addAll(getChangedNodes());
				evt.isStructureChange = true;
				triggerImageRefresh( evt);
			}
		}
	}
	
	/** A StackableStructureChange corresponds to a StackableAction (see 
	 * UndoEngine.StackableAction), meaning that performing the action over
	 * and over on the same node should consolidate into a single action. */
	public interface StackableStructureChange {
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
			this.groupTreeChange = true;
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
		public List<Node> getChangedNodes() {
			List<Node> list = new LinkedList<Node>();
			list.add(parent);
			return list;
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
			this.groupTreeChange = true;
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
		public List<Node> getChangedNodes() {
			List<Node> list = new LinkedList<Node>();
			list.add(parent);
			return list;
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
			this.groupTreeChange = true;
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
		@Override
		public List<Node> getChangedNodes() {
			List<Node> list = new LinkedList<Node>();
			list.add(oldParent);
			list.add(newParent);
			return list;
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
	
	public abstract class NodeAtributeChange extends StructureChange {
		public final Node node;
		NodeAtributeChange( Node node) {this.node = node;}
		@Override
		public List<Node> getChangedNodes() {
			List<Node> list = new LinkedList<Node>();
			list.add(node);
			return list;
		}
	}
	
	/** Changing the node's visibility */
	public class VisibilityChange extends NodeAtributeChange {
		public final boolean visibleAfter;
		
		VisibilityChange( Node node, boolean visible) {
			super(node);
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
	public class OpacityChange extends NodeAtributeChange
		implements StackableStructureChange
	{
		float opacityBefore;
		float opacityAfter;
		
		OpacityChange( Node node, float opacity) {
			super(node);
			this.opacityBefore = node.alpha;
			this.opacityAfter = opacity;
			
			this.description = "Opacity Changed";
		}

		@Override public void execute() { node.alpha = opacityAfter;}
		@Override public void unexecute() { node.alpha = opacityBefore;}
		@Override public void stackNewChange(StructureChange newChange) {
			OpacityChange change = (OpacityChange)newChange;
			this.opacityAfter = change.opacityAfter;
		}
		@Override public boolean canStack(StructureChange newChange) {
			OpacityChange change = (OpacityChange)newChange;
			return (node == change.node);
		}
	}
	
	/** Change the Workspace's Dimensions */
	public class DimensionChange extends StructureChange
	{
		int oldWidth, oldHeight;
		int newWidth, newHeight;

		DimensionChange( int width, int height) {
			this.newWidth = width;
			this.newHeight = height;
			this.oldWidth = ImageWorkspace.this.width;
			this.oldHeight = ImageWorkspace.this.height;
		}
		
		@Override
		public void execute() {
			width = newWidth;
			height = newHeight;
		}

		@Override
		public void unexecute() {
			width = oldWidth;
			height = oldHeight;
		}
		
	}
	
	public class OffsetChange extends NodeAtributeChange
		implements StackableStructureChange
	{
		int dx, dy;
		
		OffsetChange( Node node, int newX, int newY) {
			super(node);
			this.dx = newX - node.x;
			this.dy = newY - node.y;
			
			this.description = "Changed Node Offset";
		}

		@Override public void execute() { node.x += dx; node.y += dy;}
		@Override public void unexecute() { node.x -= dx; node.y -= dy;}
		@Override public void stackNewChange(StructureChange newChange) {
			OffsetChange change = (OffsetChange)newChange;
			this.dx += change.dx;
			this.dy += change.dy;
		}
		@Override public boolean canStack(StructureChange newChange) {
			OffsetChange change = (OffsetChange)newChange;
			return (node == change.node);
		}
	}
	
	/** Executes the given change and stores it in the UndoEngine */
	public void executeChange( StructureChange change) {
		change.execute();
		if( !building) {
			if( change instanceof StackableStructureChange) 
				undoEngine.storeAction(undoEngine.new StackableStructureAction(change) , null);
			else 
				undoEngine.storeAction(undoEngine.new StructureAction(change) , null);
		}
		change.alert(false);
	}
	
	/** Executes all StructureChanges (if any) in the given CompositeAction */
	public void executeComposite( CompositeAction composite) {
		for( UndoAction action : composite.getActions()) {
			if( action instanceof StructureAction) {
				StructureChange change = ((StructureAction)action).change;
				change.execute();
				change.alert(false);
			}
		}
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
		List<Node> nodes = groupTree.getRoot().getAllNodes();
		
		return nodes.contains(node);
	}
	
	// :::: Observers
	/** ImageObserver - triggers when an image's data has been changed
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
    
    private void triggerGroupStructureChanged( StructureChange evt, boolean undo) {
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