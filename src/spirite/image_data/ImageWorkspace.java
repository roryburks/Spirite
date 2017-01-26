package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.JOptionPane;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;
import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.brains.MasterControl;
import spirite.brains.SettingsManager;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.UndoEngine.CompositeAction;
import spirite.image_data.UndoEngine.NullAction;
import spirite.image_data.UndoEngine.StackableAction;
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
	private final Map<Integer,CachedImage> imageData;
	private boolean isValidHandle(ImageHandle handle) {
		return ( handle.context == this && imageData.containsKey(handle.id));
	}
	
	// Internal Components
	private final GroupTree groupTree;
	private final GroupNode referenceRoot;
	private final UndoEngine undoEngine;
	private final AnimationManager animationManager;
	private final SelectionEngine selectionEngine;
	private final DrawEngine drawEngine;
	
	// External Components
	private final CacheManager cacheManager;
	private final SettingsManager settingsManager;
	
	private GroupTree.Node selected = null;
	private int workingID = 0;	// an incrementing unique ID per imageData
	
	private int width = 0;
	private int height = 0;
	
	private File file = null;
	private boolean changed = false;
	private boolean building = true;	// While building, no UndoActionsare stored
										// and no cache is cleared
	
	
	public ImageWorkspace( MasterControl master) {
		this.cacheManager = master.getCacheManager();
		this.settingsManager = master.getSettingsManager();
		imageData = new HashMap<>();
		animationManager = new AnimationManager(this);
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this);
		selectionEngine = new SelectionEngine(this);	// Depends on UndoEngine
		drawEngine = new DrawEngine(this);	// Depends on UndoEngine, SelectionEngine
		referenceRoot = groupTree.new GroupNode("___ref");
	}
	
	@Override
	public String toString() {
		return "ImageWorkspace: " + getFileName();
	}
	
	// :::: Maintenance Methods
	public void cleanDataCache() {
		if( building) return;
		
		List<Integer> dataToRemove = new LinkedList<>();
		List<ImageHandle> undoImageSet = undoEngine.getDataUsed();
		
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
		List<List<ImageHandle>> layerDataUsed = new ArrayList<>(layers.size());
		for( int i=0; i<layers.size(); ++i) {
			layerDataUsed.add(i, ((LayerNode)layers.get(i)).getLayer().getUsedImageData());
		}
		
		// Step 1: Go through each tracked ImageData and find unused entries
		for( Integer id : imageData.keySet()) {
			ImageHandle handleToTest = new ImageHandle(this, id);
			
			if( undoImageSet.contains(handleToTest))
				continue;

			boolean used = false;
			for( List<ImageHandle> layerData : layerDataUsed) {
				if( layerData.contains(handleToTest))  {
					used = true;
					break;
				}
			}
			
			if( !used)
				dataToRemove.add(id);
		}

		// Step 2: Go through all used entries and make sure they're tracked
		for( List<ImageHandle> layerData : layerDataUsed) {
			for( ImageHandle data : layerData) {
				if( data.context != this || !imageData.containsKey(data.id))
					MDebug.handleError(ErrorType.STRUCTURAL, this, "Untracked Image Data found when cleaning ImageWorkspace.");
			}
		}
		for( ImageHandle data : undoImageSet) {
			if( data.context != this || !imageData.containsKey(data.id))
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Untracked Image Data found from UndoEngine.");
		}

		// Remove Unused Entries
		for( Integer i : dataToRemove) {
			System.out.println("Clearing Unused Workspace Data");
			imageData.get(i).relinquish(this);
			imageData.remove(i);
		}
	}
	
	CachedImage getData(int i) {
		return imageData.get(i);
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
	public GroupNode getReferenceRoot() {
		return referenceRoot;
	}
	public GroupTree getGroupTree() {
		return groupTree;
	}
	
	public List<ImageHandle> getAllImages() {
		List<ImageHandle> list = new ArrayList<>(imageData.size());
		
		for( Entry<Integer,CachedImage> entry : imageData.entrySet()) {
			list.add( new ImageHandle( this, entry.getKey()));
		}
		
		return list;
	}

	
	public ImageHandle getActiveData() {
		if( selected == null) return null;
		
		if( selected instanceof GroupTree.LayerNode) {
			// !!!! SHOULD be no reason to add sanity checks here.
			return  ((GroupTree.LayerNode)selected).getLayer().getActiveData();
		}
		return null;
	}
	public Point getActiveDataOffset() {
		if( selected instanceof GroupTree.LayerNode) {
			return new Point( selected.x, selected.y);
		}
		else
			return new Point(0,0);
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
	
	// :::: Reference Management
	private boolean editingReference = false;
	private float refAlpha = 1.0f;
	public boolean isEditingReference() {
		return editingReference;
	}
	public void setEditingReference( boolean edit) {
		if( editingReference != edit) {
			editingReference = edit;
			triggerReferenceToggle( edit);
		}
	}
	
	public float getRefAlpha() {return refAlpha;}
	public void setRefAlpha( float alpha) { 
		this.refAlpha = alpha;
		triggerReferenceStructureChanged(false);
	}
	
	
	
	public void addReferenceNode( Node toAdd, Node parent, Node before) {
		if( !verifyReference(parent) || (before != null && !verifyReference(before)))
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Non-reference attempted to insert into reference");
		else {
			parent._add(toAdd, before);
			triggerReferenceStructureChanged(true);
		}
	}
	public void clearReferenceNode( Node toRem) {
		if( !verifyReference(toRem) && toRem != null)
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Non-reference attempted to be cleared.");
		else
			toRem._del();
	}
	public boolean verifyReference( Node node) {
		int i = 0;
		
		while( i < 1000 && node != referenceRoot) {
			if( node == null || node == groupTree.getRoot())
				return false;
			
			node = node.getParent();
			++i;
		}
		if( i == 1000) {
			MDebug.handleError(ErrorType.STRUCTURAL, null, "Cyclical Node (verifyReference)");
			return false;
		}
		
		return true;
	}
	
	
	// :::: Image Checkout
	public BufferedImage checkoutImage( ImageHandle image) {
		if( !isValidHandle(image))
			return null;
		
		undoEngine.prepareContext(image);
		
		// !!! TODO: Strict image locking and unlocking seems like too much trouble.
		//	BufferedImages are buffered so you don't have to worry too much about
		//	that, but all the same I should have some way to keep track of what
		//	objects have images checked out so that they are checked in eventually
		//	and that they terminate correctly if the image were to be unloaded.
		
		return image.deepAccess();
	}
	
	public void checkinImage( ImageHandle handle) {
		if( !isValidHandle(handle))
			return;

		// Construct ImageChangeEvent and send it
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.dataChanged.add(handle);
		evt.workspace = this;
		triggerImageRefresh( evt);
	}
	
	/** Internal method should not be called by external methods (if so
	 * it'd screw up the UndoEngine).  Instead create an ImageDataReplacedAction
	 * and 
	 */
	void _replaceIamge( ImageHandle old, CachedImage newImg) {
		imageData.get(old.id).relinquish(this);
		imageData.put(old.id, newImg);
		newImg.reserve(this);
		
		
		
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.dataChanged = new  LinkedList<ImageHandle>();
		evt.dataChanged.add(old);		
		triggerImageRefresh(evt);
	}
	
	/** Performs a command of context "draw." */
	
	public void executeDrawCommand( String command) {
		
		switch( command) {
		case "undo":
			undoEngine.undo();
			break;
		case "redo":
			undoEngine.redo();
			break;
    	case "newLayerQuick":
    		setSelectedNode(
    				addNewSimpleLayer(
    						getSelectedNode(), 
    						width, height,
    						"New Layer", 
    						new Color(0,0,0,0))
    			);
    		break;
    	case "toggle_reference":
    		editingReference = !editingReference;
		case "clearLayer":
			if(!selectionEngine.attemptClearSelection()) {
				ImageHandle image = getActiveData();
				if( image != null) 
					drawEngine.clear(image);
			}
			break;
		case "cropSelection": {
			Node node = getSelectedNode();
			
			Selection selection = (selectionEngine.getSelection());
			if( selection == null) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
			
			
			Rectangle rect = selection.getBounds();
			rect.x += selectionEngine.getOffsetX();
			rect.y += selectionEngine.getOffsetY();
			

			
			cropNode( node , rect);
			break;}
		case "autocroplayer": {
			Node node = getSelectedNode();
			
			if( node instanceof LayerNode) {
				Layer layer = ((LayerNode) node).getLayer();
				
				try {
					Rectangle rect;
					rect = MUtil.findContentBounds(
							layer.getActiveData().deepAccess(),
							1, 
							false);
					cropNode((LayerNode) node, rect);
				} catch (UnsupportedDataTypeException e) {
					e.printStackTrace();
				}
				
			}
			break;}
		default:
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Draw command: draw." + command);
		}
	}
	
	
	public void cropNode( Node nodeToCrop, Rectangle bounds) {
		
		bounds = bounds.intersection(new Rectangle(0,0,width,height));
		if( bounds.isEmpty())return;
		

		if( nodeToCrop instanceof GroupNode 
			&& settingsManager.getBoolSetting("promptOnGroupCrop")) {
			int r = JOptionPane.showConfirmDialog(null, "Crop all Layers within the group?", "Cropping Group", JOptionPane.YES_NO_OPTION);
			
			if( r != JOptionPane.YES_OPTION)
				return;
		}

		List<LayerNode> toCrop = nodeToCrop.getAllLayerNodes();
		List<UndoableAction> actions = new ArrayList<UndoableAction>();
		
		for( LayerNode node : toCrop ) {
			
			List<ImageHandle> handles = node.layer.getUsedImageData();
			List<Rectangle> rects = node.layer.interpretCrop(bounds);
			
			for( int i=0; i < handles.size() && i<rects.size(); ++i) {
				Rectangle rect = new Rectangle(rects.get(i));
				ImageHandle handle = handles.get(i);
				
				
				if( rect.x < 0) {
					rect.width -= rect.x;
					rect.x = 0;
				}
				if( rect.y < 0) {
					rect.height -= rect.y;
					rect.y = 0;
				}
				
				if( rect.width <= 0 || rect.height <= 0)
					continue;
	
				if( rect.width > handle.getWidth() &&
					rect.height > handle.getHeight())
					continue;
	
				if( rect.width > handle.getWidth())
					rect.width = handle.getWidth();
				if( rect.height > handle.getHeight())
					rect.height = handle.getHeight();
				
				// Construct a crop action
				BufferedImage image = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
				MUtil.clearImage(image);
				Graphics2D g2 = (Graphics2D) image.getGraphics();
				g2.translate(-bounds.x, -bounds.y);
				handle.drawLayer(g2);
				g2.dispose();
				
				
				actions.add( undoEngine.createReplaceAction(handle, image));
			}
	
			actions.add(new StructureAction(
					new OffsetChange(node, bounds.x, bounds.y)));
		}
		
		if(!actions.isEmpty()) {
			CompositeAction action = undoEngine.new CompositeAction(actions, (nodeToCrop instanceof LayerNode) ? "Cropped Layer" : "Cropped Group");
					action.performAction();
			undoEngine.storeAction(action);
		}
	}
	
	
	
	// :::: Content Addition
	/** Imports the given Group Node and all included ImageData into it.
	 * 
	 * Importing Data works like this: it goes through the node and converts
	 * all null-context'd ImageHandles and converts them to valid handles
	 * such that they link to the the CachedImage creatted by the Data Map.
	 * 
	 * e.g. a LayerNode had a null-handle with ID 3 and in the Data Map,
	 * 	3 is mapped to a Smiley Face, importData will import the Smiley
	 * 	Face and assign it ID 7 (because ID 3 was already taken) and it will
	 * 	change the Null-Handle to a context-handle with ID 7.  All null-context
	 * 	handles with ID 3 will have the same ID (7) after Importing.
	 */
	public void importData( 
			Node node, 
			Map<Integer,BufferedImage> newData)
	{
		// Construct a list of all LayerNodes within the context.
		if( node == null) 
			node = groupTree.getRoot();
		List<Node> layers = node.getAllNodesST( new GroupTree.NodeValidatorLayer());
		if( node instanceof LayerNode)
			layers.add(node);
		
		Map<Integer,Integer> rebindMap = new HashMap<>();
		List<ImageHandle> unlinked = new ArrayList<>();
		
		// Step 1: Go through all the ImageData and find all ImageHandles
		//	that aren't active ImageHandles in the Workspace
		for( Node lnode : layers) {
			for( ImageHandle data: ((LayerNode)lnode).getLayer().getUsedImageData()) 
			{
				if( !isValidHandle(data))
					unlinked.add(data);
			}
		}
		
		// Step 2: Put the new data into the imageData map, creating
		//	a map to rebing old IDs into valid IDs
		for( Entry<Integer,BufferedImage> entry : newData.entrySet()) {
			CachedImage ci = cacheManager.cacheImage(entry.getValue(), this);
			ci.reserve(this);
			imageData.put( workingID, ci);
			rebindMap.put( entry.getKey(), workingID);
			++workingID;
		}
		
		// Step 3: Convert Null-Context ImageHandles to valid ImageHandles
		for( ImageHandle data : unlinked) {
			if( data.context != this) {
				data.id = rebindMap.get(data.id);
				data.context = this;
			}
		}
	}
	
	
	public LayerNode addNewSimpleLayer( GroupTree.Node context, BufferedImage img, String name) {
		CachedImage ci = cacheManager.cacheImage(img, this);
		ci.reserve(this);
		imageData.put( workingID, ci);
		ImageHandle handle = new ImageHandle( this, workingID);
		workingID++;

		LayerNode node = groupTree.new LayerNode( new SimpleLayer(handle), name);
		_addLayer(node, context);
		
		return node;
	}
	
	public LayerNode addNewSimpleLayer(  GroupTree.Node context, int w, int h, String name, Color c) {
		// Create new Image Data and link it to the workspace
		BufferedImage img = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.createGraphics();
        g.setColor( c);
        g.fillRect( 0, 0, width, height);
        g.dispose();
		
		return addNewSimpleLayer( 
				context, 
				img, 
				name);
	}
	
	/** A Shell Layer is a layer whose ImageHandles are not yet linked to
	 * the Workspace.  When creating a complex custom Layer or constructing
	 * multiple layers at once (e.g. when loading), it is useful to do this
	 * and then call ImportData with the Data Map to add a Layer.*/
	public LayerNode addShellLayer(GroupTree.Node context, Layer layer, String name ){
		LayerNode node = groupTree.new LayerNode( layer, name);
		
		executeChange( createAdditionChange(node, context));
		
		return node;
	}
	
	/** Internal addLayer method adds the Layer, tagging a Workspace Resize action
	 * along with it if one is warranted.
	 */
	private void _addLayer(LayerNode node, Node context) 
	{
		if( width < node.getLayer().getWidth() || height < node.getLayer().getHeight()) {
			List<UndoableAction> actions = new ArrayList<>(2);

			actions.add(new StructureAction( createAdditionChange(node,context)));
			actions.add(new StructureAction( 
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
	
	/** Adds a GroupNode at the given context. */
	public GroupTree.GroupNode addGroupNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groupTree.new GroupNode(name);
		
		executeChange(createAdditionChange(newNode,context));
		
		return newNode;
	}
	
	
	
	/** Duplicates the given node, placing its duplicate under the original. */
	public Node duplicateNode( Node toDupe) {
		if( toDupe instanceof LayerNode) {
			// Mostly duplicate code from the Layer part of bellow.
			//	Could possibly generalize
			Layer layer = ((LayerNode)toDupe).getLayer();
			Layer dupe = layer.logicalDuplicate();
			
			// Duplicate all used Data into a map
			Map< Integer, BufferedImage> dupeData = new HashMap<>();
			for( ImageHandle handle : layer.getUsedImageData()) {
				if( !dupeData.containsKey(handle.id)) {
					dupeData.put( handle.id, MUtil.deepCopy( handle.deepAccess()));
				}
			}
			
			// Import that Node
			LayerNode newNode = 
					groupTree.new LayerNode(dupe, toDupe.name + " copy");
			importData( newNode, dupeData);
			
			
			executeChange( new AdditionChange( 
					newNode, 
					toDupe.getParent(), 
					toDupe.getNextNode()));
			
			return newNode;
		}
		else if( toDupe instanceof GroupNode) {
			GroupNode dupeRoot= groupTree.new GroupNode(toDupe.name + " copy");
			Map< Integer, BufferedImage> dupeData = new HashMap<>();

			// Breadth-first queue for Duping
			Queue<NodeContext> dupeQueue = new LinkedList<NodeContext>();

			for( Node child: toDupe.getChildren()) {
				dupeQueue.add( new NodeContext(child, dupeRoot));
			}
			while( !dupeQueue.isEmpty()) {
				NodeContext next = dupeQueue.poll();
				Node dupe;
				
				if( next.toDupe instanceof GroupNode) {
					// Clone Group node, and add all its children to the queue
					dupe = groupTree.new GroupNode( next.toDupe.getName()+" copy");
					
					for( Node child : next.toDupe.getChildren()) {
						dupeQueue.add( new NodeContext( child, dupe));
					}
				}
				else {
					Layer layer = ((LayerNode)next.toDupe).getLayer();
					
					// Deep Copy any not-yet-duplicated Image data into the
					//	dupeData map
					for( ImageHandle handle : layer.getUsedImageData()) {
						if( !dupeData.containsKey(handle.id)) {
							dupeData.put( handle.id, MUtil.deepCopy( handle.deepAccess()));
						}
					}
					
					dupe = groupTree.new LayerNode( 
							layer.logicalDuplicate(), 
							next.toDupe.getName() + " copy");
				}
				
				next.parent._add(dupe, null);
			}
			
			
			importData(dupeRoot, dupeData);
			
			executeChange( new AdditionChange( 
					dupeRoot, 
					toDupe.getParent(), 
					toDupe.getNextNode()));
			
			return dupeRoot;
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
	

	/**
	 * A shallow duplication will duplicate all nodes, but all LayerNodes will
	 * be references to the original, not deep copies.  This Method does not
	 * automatically add the node to the GroupTree (as it's probably being used
	 * for something else).
	 */
	public Node shallowDuplicateNode( Node toDupe) {
		// Lots of redundant code from duplicateNode, but probably unavoidable
		
		
		if( toDupe instanceof LayerNode) 
			return groupTree.new LayerNode(((LayerNode) toDupe).getLayer(), toDupe.name);
		else if( toDupe instanceof GroupNode) {
			GroupNode dupeRoot= groupTree.new GroupNode(toDupe.name);

			// Breadth-first queue for Duping
			Queue<NodeContext> dupeQueue = new LinkedList<NodeContext>();
			
			for( Node child: toDupe.getChildren()) {
				dupeQueue.add( new NodeContext(child, dupeRoot));
			}
			while( !dupeQueue.isEmpty()) {
				NodeContext next = dupeQueue.poll();
				Node dupe;
				
				if( next.toDupe instanceof GroupNode) {
					// Clone Group node, and add all its children to the queue
					dupe = groupTree.new GroupNode( next.toDupe.getName());
					
					for( Node child : next.toDupe.getChildren()) {
						dupeQueue.add( new NodeContext( child, dupe));
					}
				}
				else {
					// Shallow-clone Layer node
					Layer layer = ((LayerNode)next.toDupe).getLayer();
					dupe = groupTree.new LayerNode( layer, next.toDupe.getName());
				}
				
				next.parent._add(dupe, null);
			}
			
			return dupeRoot;
		}
		
		return null;
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
		
		
		move( new MoveChange(
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
		
		move( new MoveChange(
				nodeToMove,
				nodeToMove.getParent(),
				nodeToMove.getNextNode(),
				nodeUnder.getParent(),
				nodeBefore));
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		if( nodeToMove == null || nodeInto == null 
				|| nodeToMove.getParent() == null || groupTree._isChild( nodeToMove, nodeInto))
			return;
		
		Node nodeBefore = null;
		if( top && !nodeInto.getChildren().isEmpty()) {
			nodeBefore = nodeInto.getChildren().get(0);
		}

		move( new MoveChange(
				nodeToMove,
				nodeToMove.getParent(),
				nodeToMove.getNextNode(),
				nodeInto,
				nodeBefore));
		
	}
	private void move( MoveChange change) {
		if( nodeInWorkspace(change.moveNode)) {
			executeChange(change);
		}
		else {
			change.execute();
			if( verifyReference(change.moveNode)) 
				triggerReferenceStructureChanged(true);
		}
	}
	
	
	// :::: Node Attribute Changes
	public void renameNode( Node node, String newName) {
		if( newName != node.name)
			executeChange( new RenameChange(newName, node));
	}

	
	/** NullActions which are handled by the ImageWorkspace are 
	 * StructureActions, whose behavior is defined by StructureChanges
	 * which are used both for the initial execution and the UndoEngine */
	public class StructureAction extends NullAction {
		public final StructureChange change;	// !!! Might be bad visibility
		
		public StructureAction(StructureChange change) {
			this.change = change;
		}
		
		@Override
		protected void performAction() {
			change.execute();
			change.alert(false);
		}
		@Override
		protected void undoAction() {
			change.unexecute();
			change.alert(true);
		}
		
		@Override
		protected void onDispatch() {
			change.cauterize();
		}
		@Override
		public String getDescription() {
			return change.description;
		}
	}
	

	
	/*** A StackableStructureAction is a StructureAction with a 
	 * StackableChange.  The StackableChange implements the stack check
	 * and merge, this class just mediates.	 */
	public class StackableStructureAction extends StructureAction
		implements StackableAction 
	{
		public StackableStructureAction(StructureChange change) {
			super(change);
		}
		@Override
		public void stackNewAction(UndoableAction newAction) {
			((StackableStructureChange)change).stackNewChange(
					((StackableStructureAction)newAction).change);
		}
		@Override
		public boolean canStack(UndoableAction newAction) {
			if( !newAction.getClass().equals(this.getClass()))
				return false;
			if( !change.getClass().equals(
					((StackableStructureAction)newAction).change.getClass()))
				return false;

			return ((StackableStructureChange)change).canStack(
					((StackableStructureAction)newAction).change);
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
		executeChange(change, true);
	}
	public void executeChange( StructureChange change, boolean addUndo) {
		change.execute();
		if( !building && addUndo) {
			if( change instanceof StackableStructureChange) 
				undoEngine.storeAction(new StackableStructureAction(change));
			else 
				undoEngine.storeAction(new StructureAction(change));
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
		GroupNode root = groupTree.getRoot();
		if( node == root) return true;
		int i = 0;
		
		while( i < 1000 && node != root) {
			if( node == null)
				return false;
			
			node = node.getParent();
			++i;
		}
		if( i == 1000) {
			MDebug.handleError(ErrorType.STRUCTURAL, null, "Cyclical Node (verifyReference)");
			return false;
		}
		
		return true;
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
    	LinkedList<ImageHandle> dataChanged = new LinkedList<>();
    	LinkedList<Node> nodesChanged = new LinkedList<>();
    	boolean selectionLayerChange = false;
    	boolean isUndoEngineEvent = false;	// Probably a more generic way to do this
    	boolean isStructureChange = false;
    	
    	public ImageWorkspace getWorkspace() { return workspace;}
    	public List<ImageHandle> getChangedImages() { return new ArrayList<>(dataChanged);}
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
    
    
    /**
     * MReferenceObservers - Triggers when the reference structure has changed,
     * when the way References are drawn is changed, or when the input method
     * is toggled between reference of non-reference.
     */
    public static interface MReferenceObserver {
    	public void referenceStructureChanged( boolean hard);
//    	public void referenceImageChanged();
    	public void toggleReference( boolean referenceMode);
    }
    private final List<MReferenceObserver> referenceObservers = new ArrayList<>();

    public void triggerReferenceStructureChanged(boolean hard) {
        for( MReferenceObserver obs : referenceObservers)
        	obs.referenceStructureChanged( hard);
    }
    private void triggerReferenceToggle(boolean edit) {
        for( MReferenceObserver obs : referenceObservers)
        	obs.toggleReference(edit);
    }

    public void addReferenceObserve( MReferenceObserver obs) { referenceObservers.add(obs);}
    public void removeReferenceObserve( MReferenceObserver obs) { referenceObservers.remove(obs); }

    
    // :::: Resource Management
    private final Map<List<ImageHandle>,List<CachedImage>> reserveMap = new HashMap<>();
    
    
    /**
     * By reserving the cache, you are preserving its state in memory until you
     * are done with it.  Be sure to call "relinquishCache" with the exact same
     * list or else the memory will never be freed.  This makes sure asynchronous
     * uses of the data (for example File Saving) does not get corrupted while 
     * not leaking access to CachedImages
     */
    public List<ImageHandle> reserveCache() {
    	List<ImageHandle> handles = new ArrayList<>(imageData.size());
    	List<CachedImage> caches = new ArrayList<>(imageData.size());
        	
    	for( Entry<Integer,CachedImage> entry : imageData.entrySet()) {
    		handles.add( new ImageHandle(this, entry.getKey()));
    		caches.add( entry.getValue());
    	}
    	for( CachedImage ci :  caches) {
    		ci.reserve(handles);
    	}
    	
    	reserveMap.put(handles, caches);

    	return handles;
    }
    
    /** Relinquish a state of  CachedImages as given by reserveCache */
    public void relinquishCache( List<ImageHandle> handles) {
    	for( CachedImage ci :  reserveMap.get(handles)) {
    		ci.relinquish(handles);
    	}
    	reserveMap.remove(handles);
    }
    
	public void cleanup() {
		for( CachedImage img : imageData.values()) {
			img.relinquish(this);
		}
		
		undoEngine.cleanup();
	}
    
    
}