package spirite.base.image_data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.brains.PaletteManager;
import spirite.base.brains.SettingsManager;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.UndoEngine.CompositeAction;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.Layer.LayerActionHelper;
import spirite.base.image_data.layers.PuppetLayer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.base.image_data.layers.SimpleLayer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.DynamicMedium;
import spirite.base.image_data.mediums.FlatMedium;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.IMedium.InternalImageTypes;
import spirite.base.image_data.mediums.PrismaticMedium;
import spirite.base.image_data.mediums.drawer.GroupNodeDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.pen.StrokeEngine;
import spirite.base.util.ObserverHandler;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;


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
public class ImageWorkspace implements MWorkspaceObserver {
	// ImageData is tracked in ImageWorkspace if it is either part of the active
	//	Image Data (used by something in the GroupTree) or it used by a component
	//	stored in the UndoEngine.
	//
	// It's possible to implement this using LinkedHashSet and a custom HashCode
	//	which would reinforce the intended purpose of the id's and prevent any
	//	chance of duplicate IDs on non-duplicate Data, but doing so would just invite
	//	hard-to-trace bugs if IDs are ever messed with too much before being tracked
	//	by ImageWorkspace.
	final Map<Integer,IMedium> mediumData;
	
	public boolean isValidHandle(MediumHandle handle) {
		return ( handle.context == this && mediumData.containsKey(handle.id));
	}
	
	// Internal Components
	private final GroupTree groupTree;
	final UndoEngine undoEngine;
	private final AnimationManager animationManager;
	private final SelectionEngine selectionEngine;
	private final ReferenceManager referenceManager;
	private final StagingManager stagingManager;
	
	// External Components
	private final SettingsManager settingsManager;
	private final RenderEngine renderEngine;
	private final PaletteManager paletteManager;
	
	private final MasterControl master;	// TODO bad-ish
	
	private GroupTree.Node selected = null;
	private int workingID = 0;	// an incrementing unique ID per imageData
	
	int width = 0;
	int height = 0;
	
	private File file = null;
	private boolean changed = false;
	private boolean building = true;	// While building, no UndoActionsare stored
										// and no cache is cleared
	
	private boolean animationView = false;
	
	
	public ImageWorkspace( MasterControl master) {
		this.settingsManager = master.getSettingsManager();
		this.renderEngine = master.getRenderEngine();
		this.paletteManager = master.getPaletteManager();
		this.master = master;
		
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this);
		animationManager = new AnimationManager(this);
		selectionEngine = new SelectionEngine(this);	// Depends on UndoEngine
		referenceManager = new ReferenceManager(this);
		stagingManager = new StagingManager(this);
		
		mediumData = new HashMap<>();
		master.addWorkspaceObserver(this);
		

	}
	@Override
	public String toString() {
		return "ImageWorkspace: " + getFileName();
	}
	
	// =============
	// ==== Maintenance Methods
	
	/** Goes through the imageData cache, removing anything that is not needed
	 * by any current Layers or anything stored in the UndoEngine */
	void cleanDataCache() {
		if( building) return;
		
		List<Integer> dataToRemove = new LinkedList<>();
		List<MediumHandle> undoImageSet = undoEngine.getDataUsed();
		
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
		List<List<MediumHandle>> layerDataUsed = new ArrayList<>(layers.size());
		for( int i=0; i<layers.size(); ++i) {
			layerDataUsed.add(i, ((LayerNode)layers.get(i)).getLayer().getImageDependencies());
		}
		
		// Step 1: Go through each tracked ImageData and find unused entries
		for( Integer id : mediumData.keySet()) {
			MediumHandle handleToTest = new MediumHandle(this, id);
			
			if( undoImageSet.contains(handleToTest))
				continue;

			boolean used = false;
			for( List<MediumHandle> layerData : layerDataUsed) {
				if( layerData.contains(handleToTest))  {
					used = true;
					break;
				}
			}
			
			if( !used)
				dataToRemove.add(id);
		}

		// Step 2: Go through all used entries and make sure they're tracked
		for( List<MediumHandle> layerData : layerDataUsed) {
			for( MediumHandle data : layerData) {
				if( data.context != this || !mediumData.containsKey(data.id))
					MDebug.handleError(ErrorType.STRUCTURAL, "Untracked Image Data found when cleaning ImageWorkspace.");
			}
		}
		for( MediumHandle data : undoImageSet) {
			if( data.context != this || !mediumData.containsKey(data.id))
				MDebug.handleError(ErrorType.STRUCTURAL, "Untracked Image Data found from UndoEngine.");
		}

		// Remove Unused Entries
		for( Integer i : dataToRemove) {
			mediumData.get(i).flush();
			mediumData.remove(i);
		}
	}
	
	// meh
	public IMedium getData(int i) {
		return mediumData.get(i);
	}
	public IMedium getData(MediumHandle handle) {
		return mediumData.get(handle.id);
	}

	int getWidthOf( int i) {
		return mediumData.get(i).getWidth();
	}
	int getHeightOf( int i) {
		return mediumData.get(i).getHeight();
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
	
	// ===========
	// ==== Getters and Setters
	public int getWidth() {return width;}
	public void setWidth( int width) {
		if( width >= 0 && this.width != width
				&& width < Globals.getMetric("workspace.max_size").width) 
		{
			this.width = width;
		}
	}
	
	public int getHeight() {return height;}
	public void setHeight( int height) {
		if( height >= 0 && this.height != height
				&& height < Globals.getMetric("workspace.max_size").height)
		{
			this.height = height;
		}
	}
	
	public boolean isUsingAnimationView() {
		return animationView;
	}
	public void setUsingAnimationView( boolean using) { 
		if( this.animationView != using) {
			this.animationView = using;
			triggerSelectedChanged();
		}
	}
	
	public File getFile() {return file;} 
	public String getFileName() {
		if( file == null) 
			return "Untitled Image";
		else
			return file.getName();
	}
	
	
	public boolean hasChanged() { return changed; }
	
	// Sub-Components
	public UndoEngine getUndoEngine() { return undoEngine; }
	public AnimationManager getAnimationManager() { return animationManager; }
	public SelectionEngine getSelectionEngine() { return selectionEngine; }
	public ReferenceManager getReferenceManager() { return referenceManager; }
	public StagingManager getStageManager() {return stagingManager;}
	public PaletteManager getPaletteManager() {return paletteManager;}	// Might be removed in the future
	
	// Super-Components (components rooted in MasterControl, simply being passed on)
	public RenderEngine getRenderEngine() { return renderEngine; }
	public SettingsManager getSettingsManager() {return master.getSettingsManager();}
	
	
	public GroupTree.GroupNode getRootNode() { return groupTree.getRoot(); }
	
	public List<MediumHandle> getAllImages() {
		List<MediumHandle> list = new ArrayList<>(mediumData.size());
		
		for( Entry<Integer,IMedium> entry : mediumData.entrySet()) {
			list.add( new MediumHandle( this, entry.getKey()));
		}
		
		return list;
	}
	
	
	// ============== Building Active Data ==================
	
	/**
	 * BuiltActiveData's should be immutable, but after the Layers give their
	 * BuiltData to the ImageWorkspace and before the ImageWorkspace gives it
	 * to the Class that requested it, ImageWorkspace has to add in the Node
	 * transform data.  BuildingActiveData is an intermediate class that 
	 * Layers send that have their local transforms.
	 * 
	 * Since this is an intermediate plain-data type intended only for 
	 * transmitting information, there's no reason to have the data protected.
	 */
	public static class BuildingMediumData {
		public final MediumHandle handle;
		
		public MatTrans trans;
		public int color;
		public BuildingMediumData( MediumHandle handle) {
			this.handle = handle;
			this.trans = new MatTrans();
		}
		public BuildingMediumData( MediumHandle handle, int ox, int oy) {
			this.handle = handle;
			this.trans = MatTrans.TranslationMatrix(ox, oy);
		}
		public void doOnBuiltData( DoOnABID doer) {
			handle.context.doOnBuiltData(this, doer);
		}
	}
	

	public IImageDrawer getActiveDrawer() {
		getSelectedNode();	// Makes sure the selected node is refreshed
		return getDrawerFromNode( selected);
	}
	public IImageDrawer getDrawerFromNode( Node node) {
		if( node instanceof LayerNode) {
			Layer layer = ((LayerNode) node).getLayer();
			BuildingMediumData bid = layer.getActiveData();

			bid.color = paletteManager.getActiveColor(0)&0xFFFFFF;	// BAD?
			IMedium medium = mediumData.get(bid.handle.id);
			return (medium == null) ? null : medium.getImageDrawer(bid);
		}
		else if( node instanceof GroupNode) {
			return new GroupNodeDrawer((GroupNode)node);
		}
		
		return null;
	}
	public IImageDrawer getDrawerFromBID( BuildingMediumData img) {
		if( img == null) return null;
		IMedium medium = mediumData.get(img.handle.id);
		return (medium == null) ? null : medium.getImageDrawer(img);
	}
	public IImageDrawer getDrawerFromHandle( MediumHandle handle) {
		IMedium medium = mediumData.get(handle.id);
		return (medium == null) ? null : medium.getImageDrawer(new BuildingMediumData(handle, 0, 0));
	}
	
	
	/** Returns the Active Data in its Built form, with all the offsets applied. */
	public BuildingMediumData buildActiveData() {
		getSelectedNode();	// Makes sure the selected node is refreshed
		if( selected == null) return null;
		
		if( selected instanceof LayerNode) {
			return buildDataFromNode( (LayerNode) selected);
		}
		return null;
	}
	
	/** Builds all applied offsets into a LayerNode. */
	public BuildingMediumData buildDataFromNode( LayerNode node) {
		BuildingMediumData data = node.getLayer().getActiveData();
		
		if( data == null) return null;
		
		data.trans.preTranslate(node.x, node.y);
		data.color = paletteManager.getActiveColor(0)&0xFFFFFF;	// BAD?
		return data;
	}
	
	/** Performs the given script using data built from the given building data. */
	public void doOnBuiltData( BuildingMediumData data, DoOnABID doer) {
		if( data == null)
			doer.Do(null);

		IMedium medium = mediumData.get(data.handle.id);
		if( medium == null)
			doer.Do(null);
		else {
			synchronized(medium) {
				floatIImage(data.handle);
				doer.Do(medium.build(data));
			}
		}
	}
	public interface DoOnABID {
		public void Do( ABuiltMediumData abid);
	}
	
	
	// =========
	// ==== Stroke Engine Piping
	StrokeEngine activeSE = null;
	public StrokeEngine getAcrtiveStrokeEngine() {
		return activeSE;
	}
	/** REALLY shouldn't be called by anything other than an IImageDrawer, but Java's mandatory package scoping is annoying. */
	public void setActiveStrokeEngine( StrokeEngine stroke) {
		if( activeSE == null || stroke == null)
			activeSE = stroke;
		else
			HybridHelper.beep();
	}
	
	
	// ==============
	// ==== Node Selection 
	
	public GroupTree.Node getSelectedNode() {
		if( !animationView)
			return getLocalSelectedNode();
		else
			return animationManager.getView().getSelectedNode();
	}
	public GroupTree.Node getLocalSelectedNode() {
		if( !nodeInWorkspace(selected))
			setSelectedNode(null);
		return selected;
		
	}
	public void setSelectedNode( GroupTree.Node node) {
		if( node != null && !nodeInWorkspace(node))  {
			MDebug.handleError(ErrorType.STRUCTURAL, null, "Tried to select a node into the wrong workspace.");
			return;
		}
		
		if( selected != node) {
			selected = node;
			triggerSelectedChanged();
		}
	}
	
	
	// ===============
	// ==== Image Checkout (called by BuiltImageData)
	/** Internal method should not be called by external methods (if so it'd
	 * 110% screw up the UndoEngine).  Instead create an ImageDataReplacedAction
	 */
	void _replaceIamge( MediumHandle oldHandle, IMedium newMedium) {
		IMedium oldMedium = mediumData.get(oldHandle.id);
		mediumData.put(oldHandle.id, newMedium.dupe());
		oldMedium.flush();
		
		oldHandle.refresh();
	}
	
	/** Creates a Group Node that's detached from the ImageWorkspace, but still
	 * has access to it.  (Mostly used for custom drawing). */
	GroupNode _createDetatchedGroup(String name) {
		return groupTree.new GroupNode(name);
	}
	
	// :::: Various Actions
	
	/**
	 *  Resizes the given node 
	 * @param nodeToCrop
	 * @param inputRect
	 * @param shrinkOnly
	 */
	public void cropNode( Node nodeToCrop, Rect inputRect, boolean shrinkOnly) {
		inputRect = inputRect.intersection(new Rect(0,0,width,height));
		if( inputRect.isEmpty())return;
		

		if( nodeToCrop instanceof GroupNode 
			&& settingsManager.getBoolSetting("promptOnGroupCrop", true)) {
			if(!HybridHelper.showConfirm("Cropping Group", "Crop all Layers within the group?"))
				return;
		}
		
		// Step 1: Crop all the Image Data in all affected ImageLayers
		List<LayerNode> toCrop = nodeToCrop.getAllLayerNodes();
		List<UndoableAction> actions = new ArrayList<>();
		List<ImageCropHelper> handlesCropped = new ArrayList<>();
		
		for( LayerNode node : toCrop ) {
			List<MediumHandle> handles = node.getLayer().getImageDependencies();
			List<Rect> imageBounds = node.getLayer().getBoundList();

			if( inputRect == null || handles  == null ) continue;
			
			for( int i=0; i < handles.size() && i<imageBounds.size(); ++i) {
				MediumHandle handle = handles.get(i);
				Rect imageBound = imageBounds.get(i);
				imageBound.x += node.x;
				imageBound.y += node.y;
				
				Rect newBounds;
				
				if( shrinkOnly) {
					newBounds = inputRect.intersection(imageBound);
				}
				else {
					newBounds = inputRect;
				}
				
				if( newBounds.equals(imageBound))
					continue;

				// Construct a crop action
				RawImage img = HybridHelper.createImage(newBounds.width, newBounds.height);
				GraphicsContext gc = img.getGraphics();
				
				gc.clear();
				MatTrans transform = new MatTrans();		
				transform.translate(imageBound.x-newBounds.x, imageBound.y-newBounds.y);
				handle.drawLayer(gc, transform);
//				g2.dispose();
				
				actions.add( undoEngine.createReplaceAction(handle, img));
				handlesCropped.add( new ImageCropHelper( handle, newBounds.x-imageBound.x,  newBounds.y-imageBound.y));
			}
	
		}
		
		// Step 2: For every Layer that uses the cropped image data, create
		//	a list of undoable actions corresponding to the structure change
		for( LayerNode node : groupTree.getRoot().getAllLayerNodes()) {
			LayerActionHelper layerAction = node.getLayer().interpretCrop(handlesCropped);
			
			if( layerAction != null ) {
				actions.addAll( layerAction.actions);
				if( layerAction.offsetChange.x !=0 || layerAction.offsetChange.y != 0) {
					actions.add( new StructureAction(new OffsetChange(node, 
							node.getOffsetX() + layerAction.offsetChange.x, 
							node.getOffsetY() + layerAction.offsetChange.y)));
				}
			}
		}
		
		if(!actions.isEmpty()) {
			CompositeAction action = undoEngine.new CompositeAction(actions, (nodeToCrop instanceof LayerNode) ? "Cropped Layer" : "Cropped Group");
			undoEngine.performAndStore(action);
		}
	}
	public static class ImageCropHelper {
		public final MediumHandle handle;
		public final int dx;
		public final int dy;
		ImageCropHelper( MediumHandle handle, int dx, int dy) {
			this.handle = handle;
			this.dx = dx;
			this.dy = dy;
		}
	}
	
	public void mergeNodes( Node source, LayerNode destination) {
		if( source == null || destination == null) return;
		
		
		if( !destination.getLayer().canMerge(source))
			return;
		
		List<UndoableAction> actions = new ArrayList<>();
		
		LayerActionHelper helper = destination.getLayer().merge(source, -destination.x+source.x, -destination.y+source.y);
		actions.addAll( helper.actions);
		if( helper.offsetChange.x != 0 || helper.offsetChange.y != 0){
			actions.add( new StructureAction(
					new OffsetChange(
							destination, 
							destination.x + helper.offsetChange.x, 
							destination.y + helper.offsetChange.y)
					));
		}
			
		actions.add(new StructureAction( 
				new DeletionChange(source, source.getParent(), source.getNextNode())));
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Merge Action"));
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
	public void importNodeWithData( 
			Node node, 
			Map<Integer,IMedium> newData)
	{
		// Construct a list of all LayerNodes within the context.
		if( node == null) 
			node = groupTree.getRoot();
		List<Node> layers = node.getAllNodesST( new GroupTree.NodeValidatorLayer());
		if( node instanceof LayerNode)
			layers.add(node);
		
		Map<Integer,Integer> rebindMap = new HashMap<>();
		List<MediumHandle> unlinked = new ArrayList<>();
		
		// Step 1: Go through all the ImageData and find all ImageHandles
		//	that aren't active ImageHandles in the Workspace
		for( Node lnode : layers) {
			for( MediumHandle data: ((LayerNode)lnode).getLayer().getImageDependencies()) 
			{
				if( !isValidHandle(data))
					unlinked.add(data);
			}
		}
		
		// Step 2: Put the new data into the imageData map, creating
		//	a map to rebing old IDs into valid IDs
		for( Entry<Integer,IMedium> entry : newData.entrySet()) {
			mediumData.put(workingID, entry.getValue());
			rebindMap.put( entry.getKey(), workingID);
			++workingID;
		}
		
		// Step 3: Convert Null-Context ImageHandles to valid ImageHandles
		for( MediumHandle data : unlinked) {
			if( data.context != this) {
				data.id = rebindMap.get(data.id);
				data.context = this;
			}
		}
	}
	
	/**
	 * Puts the newImage into the Workspace space (caching it and assigning
	 * it a handle) and returns that handle.
	 * 
	 * NOTE: If the ImageHandle is not linked to a Layer in the Workspace
	 * then the image will get flushed next time the image data is checked
	 */
	public MediumHandle importData( RawImage newImage) {
		mediumData.put( workingID, new FlatMedium(newImage, this));
		
		return new MediumHandle(this, workingID++);	// Postincriment
	}
	
	/**
	 * DynamicData resizes its area such that you never reach the "end" of 
	 * the data (but can only draw on the currently visible part) and it
	 * will automatically crop the data periodically to only the used area.
	 */
	public MediumHandle importDynamicData(RawImage newImage) {
		mediumData.put( workingID, new DynamicMedium(newImage, 0, 0, this));
		
		return new MediumHandle(this, workingID++);	// Postincriment
	}
	
	
	public LayerNode addNewSimpleLayer( GroupTree.Node contextNode, RawImage img, String name, InternalImageTypes type) {
		IMedium ii = null;
		
		switch( type) {
		case DYNAMIC:
			ii = new DynamicMedium(img, 0, 0, this);
			break;
		case PRISMATIC:
			ii = new PrismaticMedium();
			break;
		case NORMAL:
			ii = new FlatMedium(img, this);
			break;
		case MAGLEV:
			ii = new MaglevMedium(this);
			break;
		}
		
		mediumData.put( workingID, ii);
		MediumHandle handle = new MediumHandle( this, workingID);
		workingID++;

		LayerNode insertedNode = groupTree.new LayerNode( new SimpleLayer(handle), name);
		_addLayer(insertedNode, contextNode);
		
		return insertedNode;
	}
	
	public LayerNode addNewSimpleLayer(  GroupTree.Node contextNode, int w, int h, String name, int argb, InternalImageTypes type) {
		// Create new Image Data and link it to the workspace
		RawImage img = HybridHelper.createImage(w, h);
		
		GraphicsContext gc = img.getGraphics();
        gc.setColor( argb);
        gc.fillRect( 0, 0, width, height);
		
		return addNewSimpleLayer( contextNode, img, name, type);
	}
	
	public LayerNode addNewRigLayer( Node contextNode, int w, int h, String name, int argb) {
		RawImage img = HybridHelper.createImage(w, h);
		
		GraphicsContext gc = img.getGraphics();
        gc.setColor( argb);
        gc.fillRect( 0, 0, w, h);
        
        IMedium internal = new DynamicMedium(img, 0, 0, this);
        mediumData.put(workingID, internal);
        MediumHandle handle= new MediumHandle(this, workingID++);
        
		LayerNode insertedNode = groupTree.new LayerNode( new SpriteLayer(handle), name);
		_addLayer(insertedNode,contextNode);
		
		return insertedNode;
	}

	public LayerNode addNewPuppetLayer( Node contextNode, String name) {
		
		IMedium internal = new MaglevMedium(this);
		mediumData.put(workingID, internal);
		MediumHandle handle = new MediumHandle( this, workingID++);
		
		LayerNode insertedNode = groupTree.new LayerNode( new PuppetLayer(this, handle), name);
		_addLayer(insertedNode, contextNode);
		
		return insertedNode;
	}
	
	public LayerNode addNewReferenceLayer(Node context, LayerNode underlying, String name) {
		LayerNode node = groupTree.new LayerNode( new ReferenceLayer(underlying), name);
		_addLayer(node,context);
		return node;
	}
	
	
	/** A Shell Layer is a layer whose ImageHandles are not yet linked to
	 * the Workspace.  When creating a complex custom Layer or constructing
	 * multiple layers at once (e.g. when loading), it is useful to do this
	 * and then call ImportData with the Data Map to add a Layer.*/
	public LayerNode addShellLayer(GroupTree.Node contextNode, Layer layer, String name ){
		LayerNode insertedNode = groupTree.new LayerNode( layer, name);
		
		executeChange( createAdditionChange(insertedNode, contextNode));
		
		return insertedNode;
	}
	
	/** Internal addLayer method adds the Layer, tagging a Workspace Resize action
	 * along with it if one is warranted.
	 */
	private void _addLayer(LayerNode insertedNode, Node contextNode) 
	{
		if( width < insertedNode.getLayer().getWidth() || height < insertedNode.getLayer().getHeight()) {
			List<UndoableAction> actions = new ArrayList<>(2);

			actions.add(new StructureAction( createAdditionChange(insertedNode,contextNode)));
			actions.add(new StructureAction( 
					new DimensionChange( 
							Math.max(width, insertedNode.getLayer().getWidth()),
							Math.max(height, insertedNode.getLayer().getHeight()))
					));
			
			undoEngine.performAndStore(undoEngine.new CompositeAction( actions, actions.get(0).description));
		}
		else
			executeChange( createAdditionChange(insertedNode,contextNode));
	}
	
	/** Adds a GroupNode at the given context. */
	public GroupTree.GroupNode addGroupNode( GroupTree.Node context, String name) {
		GroupTree.GroupNode newNode = groupTree.new GroupNode(name);
		
		executeChange(createAdditionChange(newNode,context));
		
		return newNode;
	}
	
	public LayerNode referenceNodeDeposition( final LayerNode node, String name) {
		if( !nodeInWorkspace(node))
			return null;
		final ReferenceLayer ref = (ReferenceLayer) node.getLayer();
		undoEngine.pause();
		
		final LayerNode newNode = (LayerNode) duplicateNode(ref.getUnderlying());
		newNode.name = name;
		newNode.render.directCopy(node.render);
		newNode.x = node.x;
		newNode.y = node.y;
		
		removeNode(node);
		
		undoEngine.unpause("Convert Reference to Deep Copy");
		
		triggerSelectedChanged();
		
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
			Map< Integer, IMedium> dupeData = new HashMap<>();
			for( MediumHandle handle : layer.getImageDependencies()) {
				if( !dupeData.containsKey(handle.id)) {
					dupeData.put(handle.id, mediumData.get(handle.id).dupe());
				}
			}
			
			// Import that Node
			LayerNode newNode = 
					groupTree.new LayerNode((LayerNode) toDupe, dupe, toDupe.name + " copy");
			importNodeWithData( newNode, dupeData);
			
			executeChange( new AdditionChange( 
					newNode, 
					toDupe.getParent(), 
					toDupe.getNextNode()));
			
			return newNode;
		}
		else if( toDupe instanceof GroupNode) {
			GroupNode dupeRoot= groupTree.new GroupNode((GroupNode) toDupe, toDupe.name + " copy");
			Map< Integer, IMedium> dupeData = new HashMap<>();

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
					dupe = groupTree.new GroupNode( (GroupNode) next.toDupe, next.toDupe.getName()+" copy");
					
					for( Node child : next.toDupe.getChildren()) {
						dupeQueue.add( new NodeContext( child, dupe));
					}
				}
				else {
					Layer layer = ((LayerNode)next.toDupe).getLayer();
					
					// Deep Copy any not-yet-duplicated Image data into the
					//	dupeData map
					for( MediumHandle handle : layer.getImageDependencies()) {
						if( !dupeData.containsKey(handle.id))
							dupeData.put(handle.id, mediumData.get(handle.id).dupe());
					}
					
					dupe = groupTree.new LayerNode( 
							(LayerNode) next.toDupe,
							layer.logicalDuplicate(), 
							next.toDupe.getName() + " copy");
				}
				
				next.parent._add(dupe, null);
			}
			
			
			importNodeWithData(dupeRoot, dupeData);
			
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

				dupe.x = next.toDupe.x;
				dupe.y = next.toDupe.y;
				dupe.render.directCopy(next.toDupe.render);
				dupe.expanded = next.toDupe.expanded;
				
				
				next.parent._add(dupe, null);
			}
			
			return dupeRoot;
		}
		
		return null;
	}
	
	
	// :::: Remove Nodes
	public void removeNode( Node node) {
		if(!nodeInWorkspace(node) ) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Attempted to remove a node from the wrong Workspace.");
			return;
		}
		if( node == groupTree.getRoot()){
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Attempted to remove a Root Node.");
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
		change.execute();
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
		@Override
		public boolean reliesOnData() {
			return change.reliesOnData();
		}
		@Override
		public Collection<MediumHandle> getDependencies() {
			return change.getDependencies();
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
		public boolean reliesOnData() {return false;}
		public Collection<MediumHandle> getDependencies() { return null;}
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
			setSelectedNode(node);
			
		}
		@Override
		public void unexecute() {
			node._del();
			if( selected == node) {
				setSelectedNode(null);
			}
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
		@Override public boolean reliesOnData() {
			return true;
		}
		@Override public Collection<MediumHandle> getDependencies() {
			LinkedHashSet<MediumHandle> dependencies = new LinkedHashSet<>();
			List<LayerNode> layerNodes = node.getAllLayerNodes();
			
			for( LayerNode layerNode : layerNodes) {
				dependencies.addAll( layerNode.getLayer().getImageDependencies());
			}
			
			return dependencies;
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
			node.name = newName;
		}
		@Override
		public void unexecute() {
			node.name = oldName;
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
	public class RenderPropertiesChange extends NodeAtributeChange {
		public final RenderProperties before;
		public final RenderProperties after;
		
		RenderPropertiesChange( Node node, RenderProperties newProperties) {
			super(node);
			
			this.before = new RenderProperties( node.getRender());
			this.after = newProperties;
			this.description = "Visibility/Display Style Changed";
		}

		@Override
		public void execute() {
			node.render.alpha = after.alpha;
			node.render.visible = after.visible;
			node.render.method = after.method;
			node.render.renderValue = after.renderValue;
		}

		@Override
		public void unexecute() {
			node.render.alpha = before.alpha;
			node.render.visible = before.visible;
			node.render.method = before.method;
			node.render.renderValue = before.renderValue;
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
		if( !building && addUndo) {
			if( change instanceof StackableStructureChange) 
				undoEngine.performAndStore(new StackableStructureAction(change));
			else 
				undoEngine.performAndStore(new StructureAction(change));
		}
		else
			change.execute();
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
			nodeBefore = context;
		}

		return new AdditionChange( 
				newNode,
				parent,
				nodeBefore);
	}

	
	// ===================
	// ==== Utility Methods
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
			
			Node parent = node.getParent();
			if( parent == null || !parent.getChildren().contains(node))
				return false;
			node = parent;
			++i;
		}
		if( i == 1000) {
			MDebug.handleError(ErrorType.STRUCTURAL, null, "Cyclical Node (verifyReference)");
			return false;
		}
		
		return true;
	}
	
	
	// ========================
	// ==== Observers
	
	
	/** ImageObserver - triggers when an image's data has been changed
	 *  (<code>imageChanged</code>) or when the image's logical structure
	 *  has changed (<code>structureChanged</code>)
	 */
    public static interface MImageObserver {
        public void imageChanged(ImageChangeEvent evt);
        public void structureChanged(StructureChangeEvent evt);
    }
    public static class ImageChangeEvent {
    	ImageWorkspace workspace = null;
    	ArrayList<MediumHandle> dataChanged = new ArrayList<>();
    	ArrayList<Node> nodesChanged = new ArrayList<>();
    	boolean selectionLayerChange = false;
    	boolean isUndoEngineEvent = false;	// Probably a more generic way to do this
    	boolean isStructureChange = false;
    	
    	ImageChangeEvent(){}
    	
    	public ImageWorkspace getWorkspace() { return workspace;}
    	public List<MediumHandle> getChangedImages() { return new ArrayList<>(dataChanged);}
    	public List<Node> getChangedNodes() { return new ArrayList<>(nodesChanged);}
    	public boolean isSelectionLayerChange() { return selectionLayerChange;}
    	public boolean isStructureChange() {return isStructureChange;}
    }
    public static class StructureChangeEvent {
    	public final StructureChange change;
    	public final boolean reversed;
    	StructureChangeEvent( StructureChange change, boolean reversed) {
    		this.change = change;
    		this.reversed = reversed;
    	}
    }
    
    private final ObserverHandler<MImageObserver> imageObs = new ObserverHandler<>();
    public void addImageObserver( MImageObserver obs) { imageObs.addObserver(obs);}
    public void removeImageObserver( MImageObserver obs) { imageObs.removeObserver(obs);}

    public void triggerInternalLayerChange( Layer layer) {
    	ImageChangeEvent evt = new ImageChangeEvent();
    	evt.workspace = this;
    	evt.nodesChanged = new ArrayList<Node>(1);
    	evt.isStructureChange = true;
    	
    	for( LayerNode node : groupTree.getRoot().getAllLayerNodes()) {
    		if( node.getLayer() == layer)
    			evt.nodesChanged.add(node);
    	}
    	
    	triggerImageRefresh(evt);
        triggerFlash();
    }
    private void triggerGroupStructureChanged( StructureChange evt, boolean undo) {
    	StructureChangeEvent scevt = new StructureChangeEvent(evt,undo);
    	imageObs.trigger((MImageObserver obs)->{obs.structureChanged(scevt);});
    	
        triggerFlash();
    }
    void triggerImageRefresh(ImageChangeEvent evt) {
    	// Construct the event object
    	evt.workspace = this;
    	if( evt.selectionLayerChange ) {
    		if(selected != null && (selected instanceof LayerNode) ) {
    			BuildingMediumData bid = ((LayerNode)selected).getLayer().getActiveData();
    			if( bid != null)
    			evt.dataChanged.add( bid.handle);
    		}
    	}
    	
    	imageObs.trigger((MImageObserver obs)->{obs.imageChanged(evt);});
        
        if( evt.isUndoEngineEvent && undoEngine.atSaveSpot()) {
			changed = false;
			triggerFileChange();
        }
		else if( !changed) {
			changed = true;
			triggerFileChange();
		}
        triggerFlash();
    }

    /**
     * SelectionObserver - triggers when a different Layer has been selected
     */
    private final ObserverHandler<MNodeSelectionObserver> selectionObs = new ObserverHandler<>();
    public void addSelectionObserver( MNodeSelectionObserver obs) { selectionObs.addObserver(obs);}
    public void removeSelectionObserver( MNodeSelectionObserver obs) { selectionObs.removeObserver(obs);}
    public static interface MNodeSelectionObserver{
    	public void selectionChanged( Node newSelection);
    }
    
    private synchronized void triggerSelectedChanged() {
    	selectionObs.trigger( (MNodeSelectionObserver t) -> {t.selectionChanged(selected);});
	}
    
    
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

    private final ObserverHandler<MWorkspaceFileObserver> fileObs = new ObserverHandler<>();
    public void addWorkspaceFileObserve( MWorkspaceFileObserver obs) { fileObs.addObserver(obs);}
    public void removeWorkspaceFileObserve( MWorkspaceFileObserver obs) { fileObs.removeObserver(obs);}
    
    private synchronized void triggerFileChange() {
    	FileChangeEvent evt = new FileChangeEvent( ImageWorkspace.this, file, changed);
    	fileObs.trigger( (MWorkspaceFileObserver obs) -> { obs.fileChanged(evt);});
    }

    /** MFlashObserver is an extremely light observer that triggers any time an image has
     * been changed or whenever told to trigger from the outside.  Its primary purpose
     * is to cause the Workspace to be redrawn, e.g. from important UI settings changes.     */
	public static interface MFlashObserver {
		public void flash();
	}
	private final ObserverHandler<MFlashObserver> flashObs = new ObserverHandler<>();
    public void addFlashObserve( MFlashObserver obs) { flashObs.addObserver(obs);}
    public void removeFlashObserve( MFlashObserver obs) { flashObs.removeObserver(obs);}
	public void triggerFlash() {
		flashObs.trigger((MFlashObserver obs)->{obs.flash();});
	}
    

    // =============
    // ==== This section is 
    public class LogicalImage {
    	public int handleID;
    	public IMedium iimg;
    	private boolean floating = false;
    	
    	public void Float() {
    		if( !floating) {
    			synchronized(mediumData.get(handleID)) {
    				iimg = mediumData.get(handleID).copyForSaving();
    			}
    			floating = true;
    		}
    	}
    	public boolean isFloating() {return floating;}
    }
    
    /**
     * By reserving the cache, you are preserving its state in memory until you
     * are done with it.  Be sure to call "relinquishCache" with the exact same
     * list or else the memory will never be freed.  This makes sure asynchronous
     * uses of the data (for example File Saving) does not get corrupted while 
     * not leaking access to CachedImages
     */
    private Map<Integer,List<LogicalImage>> reserveMap = new HashMap<>();
    
    public List<LogicalImage> reserveCache() {

    	List<LogicalImage> handles = new ArrayList<>(mediumData.size());
    	for( Entry<Integer,IMedium> entry : mediumData.entrySet()) {
    		LogicalImage li = new LogicalImage();
    		li.handleID = entry.getKey();
    		handles.add(li);
    		
    		List<LogicalImage> reserves = reserveMap.get(entry.getKey());
    		if( reserves == null) {
    			reserves = new ArrayList<>(1);
    			reserveMap.put(entry.getKey(), reserves);
    		}
    		
    		reserves.add(li);
    	}

    	return handles;
    }
    
    /** If a collection of images is reserved for any reason (probably saving), changes
     * to the image should trigger a float action which will create a deep copy of the
     * image saving its previous state, otherwise it'll wait until it's needed. */
    private void floatIImage( MediumHandle handle) {
    	List<LogicalImage> reserves = reserveMap.get(handle.id);
    	if( reserves != null)
    		for( LogicalImage reserve : reserves)
    			reserve.Float();
    }
    
    /** Relinquish a state of  CachedImages as given by reserveCache */
    public void relinquishCache( List<LogicalImage> handles) {
    	for( LogicalImage toRemove : handles) {

        	Iterator<List<LogicalImage>> it = reserveMap.values().iterator(); 
        	while( it.hasNext()) {
        		List<LogicalImage> reserves = it.next();
        		
        		reserves.remove(toRemove);
        		if( reserves.isEmpty())
        			it.remove();
        	}

        	if( toRemove.iimg != null)
        		toRemove.iimg.flush();
    	}
    	for( LogicalImage limg : handles)
    		limg.iimg.flush();
    }
    
	public void cleanup() {
		for( IMedium img : mediumData.values())
			img.flush();
		
		undoEngine.cleanup();
	}
	public MediumHandle getHandleFor(IMedium iimg) {
		Iterator<Entry<Integer,IMedium>> it = mediumData.entrySet().iterator();
		
		while( it.hasNext()) {
			Entry<Integer,IMedium> entry = it.next();
			if( entry.getValue() == iimg)
				return new MediumHandle(this, entry.getKey());
		}
		
		return null;
	}
	
	
	// :: MWorkspaceObserver
	@Override	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( selected == this) 
			triggerSelectedChanged();
	}
	@Override public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override public void removeWorkspace(ImageWorkspace newWorkspace) {}
	
	public String getNonDuplicateName(String string) {
		int i = 0;
		boolean conflict = true;
		String tryName = string;
		
		List<Node> nodes = groupTree.getRoot().getAllAncestors();
		while( conflict) {
			conflict = false;
			tryName = i == 0 ? string : string + "_" + i;
			
			for( Node node : nodes) {
				if( node.getName() == tryName) {
					conflict = true;
					break;
				}
			}
			
			++i;
		}
		return tryName;
	}
	public void triggerSelectionRefresh() {
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = this;
		evt.isStructureChange = true;
		triggerImageRefresh(evt);
		
	}
	
}