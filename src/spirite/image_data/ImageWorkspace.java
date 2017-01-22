package spirite.image_data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.brains.CacheManager;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.UndoEngine.ClearAction;
import spirite.image_data.UndoEngine.CompositeAction;
import spirite.image_data.UndoEngine.StructureAction;
import spirite.image_data.UndoEngine.UndoableAction;
import spirite.image_data.layers.Layer;
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
	//
	// It's possible to implement this using LinkedHashSet and a custom HashCode
	//	which would reinforce the intended purpose of the id's and prevent any
	//	chance of duplicate IDs on non-duplicate Data, but doing so would just invite
	//	hard-to-trace bugs if IDs are ever messed with too much before being tracked
	//	by ImageWorkspace.
	Map<Integer,ImageData> imageData;	
	
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
		imageData = new HashMap<>();
		animationManager = new AnimationManager(this);
		drawEngine = new DrawEngine(this);
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this);
		selectionEngine = new SelectionEngine(this);	// Depends on UndoEngine
	}
	
	@Override
	public String toString() {
		return "ImageWorkspace: " + getFileName();
	}
	
	// :::: Maintenance Methods
	public void cleanDataCache() {
		if( building) return;
		
		Iterator<Entry<Integer, ImageData>> it = imageData.entrySet().iterator();
		LinkedHashSet<ImageData> dataToAdd = new LinkedHashSet<>();
		List<Integer> dataToRemove = new LinkedList<>();
		List<ImageData> undoImageSet = undoEngine.getDataUsed();
		
		// Create a list of all LayerNodes in the GroupTree
		List<Node> layers = groupTree.getRoot().getAllNodesST( new NodeValidator() {
			@Override
			public boolean isValid(Node node) {
				return (node instanceof LayerNode);
			}
			@Override
			public boolean checkChildren(Node node) {return true;}
		});

		// Pre-compute a list of all used data lists in case any Layer implements
		//	getUsedImageData in an inefficient way
		List<List<ImageData>> layerDataUsed = new ArrayList<>(layers.size());
		for( int i=0; i<layers.size(); ++i) {
			layerDataUsed.add(i, ((LayerNode)layers.get(i)).getLayer().getUsedImageData());
		}
		
		// Step 1: Go through each tracked ImageData and find unused entries
		while( it.hasNext()) {
			Entry<Integer, ImageData> entry = it.next();
			
			if( undoImageSet.contains(entry.getValue()))
				continue;
			
			boolean used = false;
			for( List<ImageData> layerData : layerDataUsed) {
				if( layerData.contains(entry.getValue()))  {
					used = true;
					break;
				}
			}
			
			if( !used)
				dataToRemove.add(entry.getKey());
		}

		// Step 2: Go through all used entries and make sure they're tracked
		for( List<ImageData> layerData : layerDataUsed) {
			for( ImageData data : layerData) {
				if( !imageData.containsValue(data))
					dataToAdd.add(data);
			}
		}
		for( ImageData data : undoImageSet) {
			if( !imageData.containsValue(data))
				dataToAdd.add(data);
		}

		// Add and Remove
		for( Integer i : dataToRemove) {
			imageData.remove(i);
		}
		for( ImageData data : dataToAdd) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Untracked Image Data found when cleaning ImageWorkspace.");
			if( data.id == -1 || imageData.containsKey(data.id)) {
				data.id = workingID++;
			} 
			
			imageData.put(data.id, data);
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
		
		for( ImageData data : imageData.values()) {
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
		
		return image.deepAccess();
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
		if( !imageData.containsValue(image)) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to checkout/in image from wrong workspce.");
			return false;
		}
		return true;
	}
	
	/** Performs a command of context "draw." */
	public void executeDrawCommand( String command) {
		if( command.equals("clearLayer")) {
			if(!selectionEngine.attemptClearSelection()) {
				ImageData image = getActiveData();
				if( image != null) {
					ClearAction action = undoEngine.new ClearAction(image);
					action.performImageAction(image);
					undoEngine.storeAction(action);
				}
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
	
	/** Imports the layer into the given context, assigning any ImageData contained
	 * unique identifiers.
	 * 	 */
	public LayerNode importLayer( Node context, Layer layer, String name) {
		List<ImageData> usedData = layer.getUsedImageData();
		Map<Integer,Integer> idMap = new HashMap<>();
		
		for( ImageData data : usedData) {
			if( data.id == -1) {
				data.id = workingID++;
			} else {
				// Link same-ID ImageData together
				Integer link= idMap.get(data.id);
				
				if(link == null) {
					idMap.put(data.id, workingID);
					data.id = workingID++;
				}
				else
					data.id = link;
			}
		}
		
		LayerNode node = groupTree.new LayerNode(layer, name);
		executeChange(createAdditionChange(node, context));
		return node;
	}
	
	
	/** Imports the given Group Node and all included ImageData into it.  If the
	 * ImageData has unassigned (-1) identifiers, it will automatically assign them
	 * unique ones.  If any ImageData have assigned identifiers, it will give them
	 * new ones such that any previously matching identifiers still match.
	 * 
	 * Actually I have to completely re-think the whole duplicate ID thing.  It'd
	 * have to belong in Duplicate
	 */
	public GroupNode importGroup( Node context, GroupNode node ) {
		List<Node> layers = node.getAllNodesST( new GroupTree.NodeValidatorLayer());
//		Map<Integer,ImageData> linked = new HashMap<>();
		List<ImageData> unlinked = new ArrayList<>();
		
		for( Node lnode : layers) {
			for( ImageData data: ((LayerNode)lnode).getLayer().getUsedImageData()) {
				unlinked.add(data);
			}
		}
		for( ImageData data : unlinked) {
			data.id = workingID++;
			imageData.put(data.id, data);
		}
		
		executeChange(createAdditionChange(node, context));
		return node;
	}
	
	public LayerNode addNewSimpleLayer( GroupTree.Node context, BufferedImage img, String name) {
		ImageData newImage = new ImageData( img, workingID, this);
		imageData.put(workingID, newImage);
		workingID++;

		LayerNode node = groupTree.new LayerNode( new SimpleLayer(newImage), name);
		_addLayer(node, context);
		
		return node;
	}
	
	public LayerNode addNewLayer(  GroupTree.Node context, int w, int h, String name, Color c) {
		// Create new Image Data and link it to the workspace
		ImageData data = new ImageData(w, h, c, this);
		data.id = workingID;
		imageData.put(workingID, data);
		workingID++;
		
		// Create node then execute StructureChange event
		LayerNode node = groupTree.new LayerNode( new SimpleLayer(data), name);
		_addLayer(node, context);
		
		return node;
	}
	
	public LayerNode addNewLayer( GroupTree.Node context, int identifier, String name) {
		for( ImageData data : imageData.values()) {
			if( data.id == identifier) {
				// Create node then execute StructureChange event
				LayerNode node = groupTree.new LayerNode( new SimpleLayer(data), name);

				width = Math.max(width,  data.getWidth());
				height = Math.max(height, data.getHeight());
				
				executeChange(createAdditionChange(node, context));
				
				return node;
			}
		}
		
		MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Tried to add a new Simple Layer using an identifier that does not exist in the current workspace. :" + identifier);
		
		return null;
	}
	
	/** Internal attLayer method adds the Layer, tagging a Workspace Resize action
	 * along with it if one is warranted.
	 */
	private void _addLayer(LayerNode node, Node context) 
	{
		if( width < node.getLayer().getWidth() || height < node.getLayer().getHeight()) {
			List<UndoableAction> actions = new ArrayList<>(2);

			actions.add(undoEngine.new StructureAction( createAdditionChange(node,context)));
			actions.add(undoEngine.new StructureAction( 
					new DimensionChange( 
							Math.max(width, node.getLayer().getWidth()),
							Math.max(height, node.getLayer().getHeight()))
					));
			
			CompositeAction action = undoEngine.new CompositeAction( actions, actions.get(0).description);
			executeComposite(action);
			
			// It's important that you add the action to the UndoEngine AFTER
			//	you add the node to the GroupTree by calling executingComposite
			//	otherwise the UndoEngine will cull the imageData before it's linked
			undoEngine.storeAction(action);
		}
		else
			executeChange( createAdditionChange(node,context));
	}
	
	public GroupTree.GroupNode addGroupNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groupTree.new GroupNode(name);
		
		executeChange(createAdditionChange(newNode,context));
		
		return newNode;
	}
	
	public Node duplicateNode( Node nodeToDuplicate) {
		if( nodeToDuplicate instanceof LayerNode) {
			Layer dupe = ((LayerNode)nodeToDuplicate).getLayer().duplicate();
			return importLayer(nodeToDuplicate.getNextNode(), dupe, nodeToDuplicate.getName() + " copy");
		}
		else if( nodeToDuplicate instanceof GroupNode) {
			GroupNode dupeRoot= groupTree.new GroupNode(nodeToDuplicate.name + " copy");

			// Breadth-first duping
			Queue<NodeContext> dupeQueue = new LinkedList<NodeContext>();

			for( Node child: nodeToDuplicate.getChildren()) {
				dupeQueue.add( new NodeContext(child, dupeRoot));
			}
			while( !dupeQueue.isEmpty()) {
				NodeContext next = dupeQueue.poll();
				Node dupe;
				
				if( next.toDupe instanceof GroupNode) {
					dupe = groupTree.new GroupNode( next.toDupe.getName()+" copy");
					
					for( Node child : next.toDupe.getChildren()) {
						dupeQueue.add( new NodeContext( child, dupe));
					}
				}
				else {
					dupe = groupTree.new LayerNode( ((LayerNode)next.toDupe).getLayer().duplicate(), next.toDupe.getName() + " copy");
				}
				
				next.parent._add(dupe, null);
			}
			
			return importGroup(nodeToDuplicate.getNextNode(), dupeRoot);
		}
		
		return null;
	}
	class NodeContext {
		Node toDupe, parent;
		NodeContext( Node toDupe, Node parentInDupe) {
			this.toDupe = toDupe;
			this.parent = parentInDupe;
		}
	}
	
	/***
	 * Used Primarily for Loading, this will merely add the imageData to the 
	 * Workspace's data set, but it will not be linked logically in any way
	 * (for that you'll need to add a Node to the GroupTree otherwise it
	 * will get erased next time cleanDataCache is called)
	 */
	public void addImageDataDirect( ImageData newData) {
		if( newData.id < workingID)
			newData.id = workingID++;
		else
			workingID = newData.id+1;
		imageData.put( newData.id, newData);
		
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
		private int dx, dy;
		
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
				undoEngine.storeAction(undoEngine.new StackableStructureAction(change));
			else 
				undoEngine.storeAction(undoEngine.new StructureAction(change));
		}
		change.alert(false);
	}
	
	/** Executes all StructureChanges (if any) in the given CompositeAction */
	public void executeComposite( CompositeAction composite) {
		for( UndoableAction action : composite.getActions()) {
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
	
	public StructureChange createAdditionChange( Node newNode, Node context ) {
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
    	public List<ImageData> getChangedImages() { return new ArrayList<>(dataChanged);}
    	public List<Node> getChangedNodes() { return new ArrayList<>(nodesChanged);}
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