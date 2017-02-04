package spirite.image_data;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.WeakReference;
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

import javax.activation.UnsupportedDataTypeException;
import javax.swing.JOptionPane;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;
import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.brains.MasterControl;
import spirite.brains.RenderEngine;
import spirite.brains.SettingsManager;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.UndoEngine.CompositeAction;
import spirite.image_data.UndoEngine.ImageAction;
import spirite.image_data.UndoEngine.NullAction;
import spirite.image_data.UndoEngine.StackableAction;
import spirite.image_data.UndoEngine.UndoableAction;
import spirite.image_data.layers.Layer;
import spirite.image_data.layers.Layer.LayerActionHelper;
import spirite.image_data.layers.RigLayer;
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
	private final Map<Integer,InternalImage> imageData;
	
	private class InternalImage {
		CachedImage cachedImage;
		final boolean isDynamic = false;
		InternalImage( CachedImage ci) { this.cachedImage = ci;}
		
		int getWidth() {
			return (isDynamic) ? width : cachedImage.access().getWidth();
		}
		int getHeight() {
			return (isDynamic) ? height : cachedImage.access().getHeight();
		}
	}
	
	private boolean isValidHandle(ImageHandle handle) {
		return ( handle.context == this && imageData.containsKey(handle.id));
	}
	
	// Internal Components
	private final GroupTree groupTree;
	private final UndoEngine undoEngine;
	private final AnimationManager animationManager;
	private final SelectionEngine selectionEngine;
	private final DrawEngine drawEngine;
	private final ReferenceManager referenceManager;
	
	// External Components
	private final CacheManager cacheManager;
	private final SettingsManager settingsManager;
	private final RenderEngine renderEngine;
	
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
		this.renderEngine = master.getRenderEngine();
		imageData = new HashMap<>();
		animationManager = new AnimationManager(this);
		groupTree = new GroupTree(this);
		undoEngine = new UndoEngine(this);
		selectionEngine = new SelectionEngine(this);	// Depends on UndoEngine
		drawEngine = new DrawEngine(this);	// Depends on UndoEngine, SelectionEngine
		referenceManager = new ReferenceManager(this);
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
			layerDataUsed.add(i, ((LayerNode)layers.get(i)).getLayer().getImageDependencies());
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
			imageData.get(i).cachedImage.relinquish(this);
			imageData.remove(i);
		}
	}
	
	CachedImage getData(int i) {
		return imageData.get(i).cachedImage;
	}

	int getWidthOf( int i) {
		return imageData.get(i).getWidth();
	}
	int getHeightOf( int i) {
		return imageData.get(i).getHeight();
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
	
	public ReferenceManager getReferenceManager() {
		return referenceManager;
	}
	
	// Doesn't feel great leaking external components, but they're very
	//	relevant to images and it's better than given them MasterControl
	public CacheManager getCacheManager() {
		return cacheManager;
	}
	public RenderEngine getRenderEngine() {
		return renderEngine;
	}
	
	public GroupTree.GroupNode getRootNode() {
		return groupTree.getRoot();
	}
	public GroupTree getGroupTree() {
		return groupTree;
	}
	
	public List<ImageHandle> getAllImages() {
		List<ImageHandle> list = new ArrayList<>(imageData.size());
		
		for( Entry<Integer,InternalImage> entry : imageData.entrySet()) {
			list.add( new ImageHandle( this, entry.getKey()));
		}
		
		return list;
	}

	
	/**
	 * A BuildActiveData is a helper class which is intended to be used
	 * by Image-modification functions which operate in Image Space.  It
	 * combines all applied transform properties such that when a function 
	 * draws on the BuiltActiveData at X, Y, it'll be modifying the ImageData
	 * which APPEARS at X,Y.
	 * 
	 * Right now only incorporates Offset, but can be modified to incorperate
	 * any kind of draw action in the future.
	 */
	public class BuiltImageData {
		public final ImageHandle handle;
		private final int ox;
		private final int oy;

		private BufferedImage working = null;
		private Graphics g = null;
		
		public BuiltImageData( ImageHandle handle) {
			this.handle = handle;
			this.ox = 0;
			this.oy = 0;
		}
		public BuiltImageData( ImageHandle handle, int ox, int oy) {
			this.handle = handle;
			this.ox = ox;
			this.oy = oy;
		}
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			
			AffineTransform transform = g2.getTransform();
			g2.translate(ox, oy);
			
			handle.drawLayer(g2);
			
			g2.setTransform( transform);
		}
		
		public void drawBorder( Graphics g) {
			if( handle == null) return;
			
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform transform = g2.getTransform();
			g2.translate(ox, oy);
			
			g2.drawRect(0, 0, handle.getWidth(), handle.getHeight());
			
			g2.setTransform( transform);
		}
		
		/**
		 * Creates a graphical object with transforms applied such that
		 * drawing on the returned Graphics will draw on the correct Image
		 * Data spot.
		 * 
		 * !!! When done modifying the image always call checkout. !!!
		 */
		public Graphics checkout() {
			if( handle.context != ImageWorkspace.this)
				MDebug.handleError(ErrorType.STRUCTURAL, null, "Checking out image in wrong workspace");
			
			InternalImage internal = imageData.get(handle.id);
			
			if( internal.isDynamic) {
				undoEngine.prepareContext(handle);
				if( working != null)
					MDebug.handleError(ErrorType.STRUCTURAL, null, "Tried to double-checkout a dynamic image.");
				
				working = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = working.getGraphics();
				draw(g);
				return g;
			}
			else {
				BufferedImage bi = checkoutImage(handle);
				g = bi.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				g2.translate(-ox, -oy);
				return g;
			}
		}
		
		/** Retrieves the underlying BufferedImage of the BuiltImage
		 * 
		 * !!! When done modifying the image always call checkout. !!!
		 */
		public BufferedImage checkoutRaw() {
			InternalImage internal = imageData.get(handle.id);
			
			if( internal.isDynamic) {
				undoEngine.prepareContext(handle);
				if( working != null)
					MDebug.handleError(ErrorType.STRUCTURAL, null, "Tried to double-checkout a dynamic image.");

				working = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = working.getGraphics();
				draw(g);
				g.dispose();
				return working;
			}
			else
				return checkoutImage(handle);
		}
		
		/**
		 * Once finished drawn you must checkin your data.  Not only does this
		 * dispose the Graphics (which is debatably necessary), but it triggers
		 * the appropriate ImageChange actions.  And if the ImageHandle is 
		 * Dynamic, it's VERY important to call this so that it gets anchored
		 * correctly
		 */
		public void checkin() {
			InternalImage internal = imageData.get(handle.id);
			
			if( internal.isDynamic) {
				// Reset all draw properties.  There might be a better way.
			//	if( g != null)
		//			g.dispose();
	//			g = working.getGraphics();
				
				// Draws the old data, as built ontop of the new
//				draw(g);
//				g.dispose();
				
				
				
				try {
					Rectangle rect = MUtil.findContentBounds(working, 0, true);
					
					if( !rect.isEmpty()) {
						BufferedImage bi = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
						Graphics g = bi.getGraphics();	// Over-writing scope not strictly necessary
						g.drawImage(working, -rect.x, -rect.y, null);
						g.dispose();
						_replaceIamge(handle, cacheManager.cacheImage(bi, ImageWorkspace.this));
					}
				} catch (UnsupportedDataTypeException e) {
					e.printStackTrace();
					MDebug.handleError(ErrorType.STRUCTURAL, e, "Bad ImageDataType on Dynamic Image Re-bound");
				}
				working.flush();
				working = null;
				

				ImageChangeEvent evt = new ImageChangeEvent();
				evt.dataChanged.add(handle);
				evt.workspace = ImageWorkspace.this;
				triggerImageRefresh( evt);
			}
			else if( g == null)	{// Should only happen if it was a raw checkout.
				checkinImage(handle);
			}
			else {
				checkinImage(handle);
				g.dispose();
			}
			g = null;
		}
		
		/** Converts the given point in ImageSpace to BuiltActiveData space*/
		public Point convert( Point p) {
			//	Some image modification methods do not use draw actions, but
			//	 rather alter the image directly.  For example a flood fill action.
			//	
			return new Point(p.x-ox, p.y-oy);
		}
		
		/** Returns the Tranform needed to convert WorkspaceSpace into DataSpace*/
		public AffineTransform getTransform() {
			AffineTransform transform = new AffineTransform();
			transform.translate( -ox, -oy);
			return transform;
		}
	}

	/**
	 * BuiltActiveData's should be immutable, but after the Layers give their
	 * BuiltData to the ImageWorkspace and before the ImageWorkspace gives it
	 * to the Class that requested it, ImageWorkspace has to add in the Node
	 * transform data.  BuildingActiveData is an intermediate class that 
	 * Layers send that have their local transforms.
	 */
	public static class BuildingImageData {
		public final ImageHandle handle;
		private final int ox;
		private final int oy;
		public BuildingImageData( ImageHandle handle, int ox, int oy) {
			this.handle = handle;
			this.ox = ox;
			this.oy = oy;
		}
	}
	
	public boolean isActiveHandle( ImageHandle handle) {
		if( selected == null) return false;
		
		if( selected instanceof LayerNode) {
			return ((LayerNode)selected).getLayer().getActiveData().handle.equals( handle);
		}
		return false;
	}
	
	public BuiltImageData buildActiveData() {
		getSelectedNode();	// Makes sure the selected node is refreshed
		if( selected == null) return null;
		
		if( selected instanceof LayerNode) {
			BuildingImageData data = ((LayerNode)selected).getLayer().getActiveData();
			
			if( data == null) return null;
			return  new BuiltImageData( data.handle,
					data.ox + selected.x, data.oy + selected.y);
		}
		return null;
	}
	
	public BuiltImageData buildData( LayerNode node) {
		getSelectedNode();	// Makes sure the selected node is refreshed
		BuildingImageData data = node.getLayer().getActiveData();
		
		return  new BuiltImageData( data.handle,
				data.ox + selected.x, data.oy + selected.y);
	}
	
	
/*	public Point getActiveDataOffset() {
		if( selected instanceof GroupTree.LayerNode) {
			return new Point( selected.x, selected.y);
		}
		else
			return new Point(0,0);
	}*/
	
	public GroupTree.Node getSelectedNode() {
		if( !nodeInWorkspace(selected)) {
			setSelectedNode(null);
		}
		return selected;
	}
	public void setSelectedNode( GroupTree.Node node) {
		if( node != null && !nodeInWorkspace(node))  {
			MDebug.handleError(ErrorType.STRUCTURAL, null, "Tried to select a node into the wrong workspace.");
			return;
		}
		
		if( selected != node) {
			if( selectionEngine.isLifted()) {
//				selectionEngine.
			}
			
			selected = node;
			triggerSelectedChanged();
		}
	}
	
	
	// :::: Image Checkout
	private BufferedImage checkoutImage( ImageHandle image) {
		if( !isValidHandle(image))
			return null;
		
		undoEngine.prepareContext(image);
		
		// !!! TODO: Strict image locking and unlocking seems like too much trouble.
		//	BufferedImages are buffered so you don't have to worry too much about
		//	that, but all the same I should have some way to keep track of what
		//	objects have images checked out so that they are checked in eventually
		//	and that they terminate correctly if the image were to be unloaded.
		
		InternalImage internalImage = imageData.get(image.id);
		return internalImage.cachedImage.access();
	}
	
	private void checkinImage( ImageHandle handle) {
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
		InternalImage internal = imageData.get(old.id);
		
		internal.cachedImage.relinquish(this);
		internal.cachedImage = newImg;
		newImg.reserve(this);
		
		
		
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.dataChanged = new  ArrayList<ImageHandle>(1);
		evt.dataChanged.add(old);		
		triggerImageRefresh(evt);
	}
	
	/** I don't like that this function exists, but it was naive to think I
	 * could avoid it. 
	 * ONLY USE IF YOU REALLY NEED THE CACHEDIMAGE, NOT JUST THE BUFFEREDIMAGE*/
	CachedImage _accessCache( ImageHandle handle) {
		return imageData.get(handle.id).cachedImage;
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
	public void cropNode( Node nodeToCrop, Rectangle inputRect, boolean shrinkOnly) {
		
		inputRect = inputRect.intersection(new Rectangle(0,0,width,height));
		if( inputRect.isEmpty())return;
		

		if( nodeToCrop instanceof GroupNode 
			&& settingsManager.getBoolSetting("promptOnGroupCrop")) {
			int r = JOptionPane.showConfirmDialog(null, "Crop all Layers within the group?", "Cropping Group", JOptionPane.YES_NO_OPTION);
			
			if( r != JOptionPane.YES_OPTION)
				return;
		}
		
		// Step 1: Crop all the Image Data in all affected ImageLayers
		List<LayerNode> toCrop = nodeToCrop.getAllLayerNodes();
		List<UndoableAction> actions = new ArrayList<>();
		List<ImageCropHelper> handlesCropped = new ArrayList<>();
		
		for( LayerNode node : toCrop ) {
			List<ImageHandle> handles = node.layer.getImageDependencies();
			List<Rectangle> imageBounds = node.layer.getBoundList();

			if( inputRect == null || handles  == null ) continue;
			
			for( int i=0; i < handles.size() && i<imageBounds.size(); ++i) {
				ImageHandle handle = handles.get(i);
				Rectangle imageBound = imageBounds.get(i);
				imageBound.x += node.x;
				imageBound.y += node.y;
				
				Rectangle newBounds;
				
				if( shrinkOnly) {
					newBounds = inputRect.intersection(imageBound);
				}
				else {
					newBounds = inputRect;
				}
				
				if( newBounds.equals(imageBound))
					continue;

				// Construct a crop action
				BufferedImage image = new BufferedImage( 
						newBounds.width, newBounds.height, BufferedImage.TYPE_INT_ARGB);
				MUtil.clearImage(image);
				Graphics2D g2 = (Graphics2D) image.getGraphics();
				g2.translate(imageBound.x-newBounds.x, imageBound.y-newBounds.y);
				handle.drawLayer(g2);
				g2.dispose();
				
				actions.add( undoEngine.createReplaceAction(handle, image));
				handlesCropped.add( new ImageCropHelper( handle, newBounds.x-imageBound.x,  newBounds.y-imageBound.y));
			}
	
		}
		
		// Step 2: For every Layer that uses the cropped image data, create
		//	a list of undoable actions corresponding to the structure change
		for( LayerNode node : groupTree.getRoot().getAllLayerNodes()) {
			LayerActionHelper layerAction = node.getLayer().interpretCrop(handlesCropped);
			
			if( layerAction != null ) {
				actions.addAll( layerAction.actions);
				if( !layerAction.offsetChange.equals(MUtil.ORIGIN)) {
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
		public final ImageHandle handle;
		public final int dx;
		public final int dy;
		ImageCropHelper( ImageHandle handle, int dx, int dy) {
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
		CompositeAction composite = undoEngine.new CompositeAction(actions, "Merge Action");
		composite.performAction();
		undoEngine.storeAction(composite);
	}
	
	
	/** 
	 * Shifts all image data of the node.
	 */
	public void shiftData( Node target, int shiftX, int shiftY) {
		List<LayerNode> layerNodes = target.getAllLayerNodes();
		LinkedHashSet<ImageHandle> data = new LinkedHashSet<>();
		
		for( LayerNode node : layerNodes) {
			data.addAll( node.getLayer().getImageDependencies());
		}

		if( data.isEmpty())
			return;
		List<UndoableAction> actions = new LinkedList<>();
		for( ImageHandle handle : data) 
			actions.add(new ShiftDataAction(handle, shiftX, shiftY));
		
		CompositeAction composte = undoEngine.new StackableCompositeAction(actions, "Shift Image Data");
		composte.performAction();
		
		undoEngine.storeAction(composte);
	}
	
	private class ShiftDataAction extends ImageAction
		implements StackableAction
	{
		private int x, y;
		protected ShiftDataAction(ImageHandle data, int x, int y) {
			super( new BuiltImageData(data));
			this.x = x;
			this.y = y;
		}
		@Override
		protected void performImageAction() {
			BufferedImage img = builtImage.checkoutRaw();
			BufferedImage buffer = new BufferedImage( img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			MUtil.clearImage(buffer);
			Graphics g = buffer.getGraphics();
			g.drawImage(img, x, y, null);
			g.dispose();
			MUtil.clearImage(img);
			g = img.getGraphics();
			g.drawImage(buffer, 0, 0, null);
			g.dispose();
			builtImage.checkin();
		}
		@Override
		public void stackNewAction(UndoableAction newAction) {
			ShiftDataAction other = (ShiftDataAction) newAction;
			x += other.x;
			y += other.y;
		}
		@Override
		public boolean canStack(UndoableAction action) {
			if(! (action instanceof ShiftDataAction)) return false;
			ShiftDataAction other = (ShiftDataAction) action;

			if( other.builtImage.handle != this.builtImage.handle) return false;
			if( other.x <0 && x > 0) return false;
			if( other.x >0 && x < 0) return false;
			if( other.y >0 && y < 0) return false;
			if( other.y <0 && y > 0) return false;
			
			return true;
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
	public void importNodeWithData( 
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
			for( ImageHandle data: ((LayerNode)lnode).getLayer().getImageDependencies()) 
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
			
			imageData.put( workingID, new InternalImage(ci));
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
	
	/**
	 * Puts the newImage into the Workspace space (caching it and assigning
	 * it a handle) and returns that handle.
	 * 
	 * NOTE: If the ImageHandle is not linked to a Layer in the Workspace
	 * then the image will get flushed next time the image data is checked
	 */
	public ImageHandle importData( BufferedImage newImage) {
		CachedImage ci = cacheManager.cacheImage(newImage, this);
		imageData.put( workingID, new InternalImage(ci));
		ci.reserve(this);
		
		return new ImageHandle(this, workingID++);	// Postincriment
	}
	
	
	public LayerNode addNewSimpleLayer( GroupTree.Node context, BufferedImage img, String name) {
		CachedImage ci = cacheManager.cacheImage(img, this);
		ci.reserve(this);
		imageData.put( workingID, new InternalImage(ci));
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
	
	public LayerNode addNewRigLayer( Node context, int w, int h, String name, Color c) {
		BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		CachedImage ci = cacheManager.cacheImage( bi, this);
        Graphics g = bi.createGraphics();
        g.setColor( c);
        g.fillRect( 0, 0, w, h);
        g.dispose();
        
        InternalImage internal = new InternalImage(ci);
//        internal.isDynamic = true;
        imageData.put(workingID, internal);
        ci.reserve(this);
        ImageHandle handle= new ImageHandle(this, workingID++);
        
		LayerNode node = groupTree.new LayerNode( new RigLayer(handle), name);
		_addLayer(node,context);
		
		return node;
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
			for( ImageHandle handle : layer.getImageDependencies()) {
				if( !dupeData.containsKey(handle.id)) {
					dupeData.put( handle.id, MUtil.deepCopy( handle.deepAccess()));
				}
			}
			
			// Import that Node
			LayerNode newNode = 
					groupTree.new LayerNode(dupe, toDupe.name + " copy");
			importNodeWithData( newNode, dupeData);
			
			
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
					for( ImageHandle handle : layer.getImageDependencies()) {
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

				dupe.alpha = next.toDupe.alpha;
				dupe.x = next.toDupe.x;
				dupe.y = next.toDupe.y;
				dupe.visible = next.toDupe.visible;
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
		change.execute();
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
		@Override
		public boolean reliesOnData() {
			return change.reliesOnData();
		}
		@Override
		public Collection<ImageHandle> getDependencies() {
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
		public Collection<ImageHandle> getDependencies() { return null;}
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
		@Override public Collection<ImageHandle> getDependencies() {
			LinkedHashSet<ImageHandle> dependencies = new LinkedHashSet<>();
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
			nodeBefore = context;
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
    	ArrayList<ImageHandle> dataChanged = new ArrayList<>();
    	ArrayList<Node> nodesChanged = new ArrayList<>();
    	boolean selectionLayerChange = false;
    	boolean isUndoEngineEvent = false;	// Probably a more generic way to do this
    	boolean isStructureChange = false;
    	
    	ImageChangeEvent(){}
    	
    	public ImageWorkspace getWorkspace() { return workspace;}
    	public List<ImageHandle> getChangedImages() { return new ArrayList<>(dataChanged);}
    	public List<Node> getChangedNodes() { return new ArrayList<>(nodesChanged);}
    	public boolean isSelectionLayerChange() { return selectionLayerChange;}
    	public boolean isStructureChange() {return isStructureChange;}
    }
    List<WeakReference<MImageObserver>> imageObservers = new ArrayList<>();

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
    }
    private void triggerGroupStructureChanged( StructureChange evt, boolean undo) {
    	Iterator<WeakReference<MImageObserver>> it = imageObservers.iterator();
    	
    	while( it.hasNext()) {
    		MImageObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else
    			obs.structureChanged( evt);
    	}
    }
    synchronized void triggerImageRefresh(ImageChangeEvent evt) {
    	Iterator<WeakReference<MImageObserver>> it = imageObservers.iterator();
    	
    	if( evt.selectionLayerChange ) {
    		BuiltImageData bid = buildActiveData();
    		if( bid != null) {
    			evt.dataChanged.add( bid.handle);
    		}
    		
    	}
    	
    	while( it.hasNext()) {
    		MImageObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else
    			obs.imageChanged(evt);
    	}
        
        if( evt.isUndoEngineEvent && undoEngine.atSaveSpot()) {
			changed = false;
			triggerFileChange();
        }
		else if( !changed) {
			changed = true;
			triggerFileChange();
		}
    }

    public void addImageObserver( MImageObserver obs) { imageObservers.add(new WeakReference<>(obs));}
    public void removeImageObserver( MImageObserver obs) { 
    	Iterator<WeakReference<MImageObserver>> it = imageObservers.iterator();
    	while( it.hasNext()) {
    		MImageObserver other= it.next().get();
    		if( other == obs || other == null)
    			it.remove(); 
    	}
    }
    


    /**
     * SelectionObserver - triggers when a different Layer has been selected
     */
    public static interface MSelectionObserver{
    	public void selectionChanged( Node newSelection);
    }
    List<WeakReference<MSelectionObserver>> selectionObservers = new ArrayList<>();
    
    private synchronized void triggerSelectedChanged() {
    	Iterator<WeakReference<MSelectionObserver>> it = selectionObservers.iterator();
    	while( it.hasNext()) {
    		MSelectionObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.selectionChanged( selected);
    	}
	}

    public void addSelectionObserver( MSelectionObserver obs) { selectionObservers.add(new WeakReference<>(obs));}
    public void removeSelectionObserver( MSelectionObserver obs) { 
    	Iterator<WeakReference<MSelectionObserver>> it = selectionObservers.iterator();
    	while( it.hasNext()) {
    		MSelectionObserver other = it.next().get();
    		if( other == obs || other == null)
    			it.remove();
    	}
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
    List<WeakReference<MWorkspaceFileObserver>> fileObservers = new ArrayList<>();
    
    private synchronized void triggerFileChange() {
    	Iterator<WeakReference<MWorkspaceFileObserver>> it = fileObservers.iterator();
    	while( it.hasNext()) {
    		MWorkspaceFileObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else 
    			obs.fileChanged( new FileChangeEvent( ImageWorkspace.this, file, changed));
    	}
    }

    public void addWorkspaceFileObserve( MWorkspaceFileObserver obs) { fileObservers.add(new WeakReference<>(obs));}
    public void removeWorkspaceFileObserve( MWorkspaceFileObserver obs) {
    	Iterator<WeakReference<MWorkspaceFileObserver>> it = fileObservers.iterator();
    	while( it.hasNext()) {
    		MWorkspaceFileObserver other = it.next().get();
    		if( other == null || other == obs)
    			fileObservers.remove(obs); 
    	}
    }
    
    

    
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
        	
    	for( Entry<Integer,InternalImage> entry : imageData.entrySet()) {
    		handles.add( new ImageHandle(this, entry.getKey()));
    		caches.add( entry.getValue().cachedImage);
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
		for( InternalImage img : imageData.values()) {
			img.cachedImage.relinquish(this);
		}
		
		undoEngine.cleanup();
	}
    
	
	// DEBUG
	List<Node> toggleList = new ArrayList<>();
	int toggleid = 0;
	
	public void toggleQuick() {
		for( Node n : toggleList) {
			n.alpha = 0.3f;
		}
		
		toggleid++;
		if( toggleid >= toggleList.size())
			toggleid = 0;
		
		if( toggleid < toggleList.size())
			toggleList.get(toggleid).alpha = 1.0f;
		
		ImageChangeEvent evt = new ImageChangeEvent();
		evt.workspace = this;
		evt.nodesChanged = new ArrayList<>(toggleList);
		triggerImageRefresh(evt);
	}
	
	public void addToggle( Node node) {
		toggleList.add(node);
	}
	
	public void remToggle( Node node) {
		toggleList.clear();;
	}
    
}