package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import spirite.MUtil;
import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.UndoEngine.ImageAction;
import spirite.image_data.UndoEngine.NullAction;
import spirite.image_data.UndoEngine.StackableAction;
import spirite.image_data.UndoEngine.UndoableAction;

/***
 *  The SelectionEngine controls the selected image data, moving it from
 *  layer to layer, workspace to workspace, and program-to-clipboard
 *  
 *  A "Selection" is essentially an alpha mask and a corresponding 
 *  BufferedImage which is floating outside of the ImageWorkspace until
 *  it is either merged into existing data or elevated to its own layer.
 *  
 *  I debated on whether or not to integrate offsets into the Selection
 *  rather than have it tracked separately, but offsets would still need
 *  to be tracked for liftedData so there would be no point.
 *  
 * @author Rory Burks
 *
 */
public class SelectionEngine {
	public enum SelectionType {
		RECTANGLE,
		
	}
	
	private final ImageWorkspace workspace;
	private final UndoEngine undoEngine;
	private final CacheManager cacheManager;
	
	// Variables related to Selection state
	private final SelectionScope scope = new SelectionScope();
	class SelectionScope {
		private Selection selection = null;
		private boolean lifted = false;
		protected CachedImage liftedImage = null;
		protected int offsetX;
		protected int offsetY;
		
		private Selection getSelection() {return selection;}
		private boolean isLifted() {return lifted;}
		private CachedImage getLiftedImage() {return liftedImage;}
		private int getOffsetX() {return offsetX;}
		private int getOffsetY() {return offsetY;}
		
		private void setFromBuiltSelection(BuiltSelection sel) {
			selection = sel.selection;
			offsetX = sel.offsetX;
			offsetY = sel.offsetY;
		}
	}
	private StartSelectionAction startAction = null;
	
	// Stored for the UndoEngine
	private BuiltSelection oldSelection = null;
	
	// Variables relating to Building
	private boolean building = false;
	private SelectionType selectionType;	// Only used when building a selection
	private Selection buildingSelection = null;
	private int startX;
	private int startY;
	private int currentX;
	private int currentY;
	
	SelectionEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();
		this.cacheManager = workspace.getCacheManager();
	}

	// :::: Get/Set
	public boolean isLifted() {
		return scope.isLifted();
	}
	
	public boolean isBuilding() {
		return building;
	}
	public Selection getBuildingSelection() {
		return buildingSelection;
	}
	
	/** Returns a BuiltSelection which incorporates all relevant offset data
	 * such that Workspace space is converted into Selection space on the
	 * node it's selecting.
	 * 
	 * (Consider incorperating SelectedNode into this?)
	 */
	public BuiltSelection getBuiltSelection() {
		return new BuiltSelection( 
				scope.getSelection(), 
				scope.getOffsetX(), 
				scope.getOffsetY());
	}
	public Selection getSelection() {
		return scope.getSelection();
	}
	public CachedImage getLiftedImage() {
		return scope.getLiftedImage();
	}
	public int getOffsetX() {
		return (building)?currentX:scope.getOffsetX();
	}
	public int getOffsetY() {
		return (building)?currentY:scope.getOffsetY();
	}
	public void setOffset( int x, int y) {
		int dx = x - scope.getOffsetX();
		int dy = y - scope.getOffsetY();

		if( !scope.isLifted()) {
			if( !validateSelection())
				return;
			
			Node snode = workspace.getSelectedNode();
			if( !(snode instanceof LayerNode))
				return;
			LayerNode node = (LayerNode)snode;

			StartSelectionAction startAction = createLiftAction(node);
			
			// Construct an execute the composite action
			List<UndoableAction> actions = new ArrayList<>(3);
			actions.add( startAction);
			// TODO: Verify that it works
			actions.add(new ClearSelectionAction(
					getBuiltSelection(),
					workspace.buildData(node)));
			actions.add( new MoveSelectionAction(dx,dy));
			
			UndoableAction action = undoEngine.new StackableCompositeAction(actions, actions.get(2).description);
			action.performAction();
			undoEngine.storeAction(action);
		}
		else {
			UndoableAction action = new MoveSelectionAction(dx, dy);
			action.performAction();
			undoEngine.storeAction(action);
		}
	}
	
	
	public boolean attemptClearSelection() {
		if( scope.isLifted()) {
			// If you have lifted data, it clears the lifted data without changing the
			//	 fact that it is lifted
			UndoableAction action = new EndSelectionAction(startAction);
			action.performAction();
			undoEngine.storeAction( action);
			return true;
		}
		
		if(!validateSelection())
			return false;
		

		Node snode = workspace.getSelectedNode();
		if( !(snode instanceof LayerNode))
			return false;
		LayerNode node = (LayerNode)snode;
		
		
		ImageAction action = new ClearSelectionAction(
				getBuiltSelection(),
				workspace.buildData(node));
		action.performImageAction();
		undoEngine.storeAction(action);
		return true;
	}
	
	
	// :::: Selection Building
	public void startBuildingSelection( SelectionType type, int x, int y) {
		// !!!! For the engine to work as expected, it is important that the user
		//	only updates the selection through these building mechanisms.
		
		oldSelection = getBuiltSelection();
		
		// Start building
		building = true;
		selectionType = type;
		startX = x;
		startY = y;
		buildingSelection = new NullSelection();
	}
	
	public void updateBuildingSelection( int x, int y) {
		if(!building) return;
		
		SelectionEvent evt = new SelectionEvent();
		
		switch( selectionType) {
		case RECTANGLE:
			buildingSelection = new RectSelection( Math.abs(startX-x), Math.abs(startY-y));
			currentX = Math.min(startX, x);
			currentY = Math.min(startY, y);
			evt.selection = buildingSelection;
			break;
		}
		
		triggerBuildingSelection( evt);
		
	}

	public void finishBuildingSelection() {
		// First Verify that the selection is non-empty
		building = false;
		
		Rectangle rect = buildingSelection.clipToRect(new Rectangle( -currentX, -currentY, workspace.getWidth(), workspace.getHeight()));
		
		if( rect == null) {
			unselect();
			return;
		}
		
		currentX += rect.x;
		currentY += rect.y;
		
		// Then Store the action and perform it
		UndoableAction action = createNewSelect(buildingSelection, currentX, currentY);
		action.performAction();
		undoEngine.storeAction(  action);
		
		triggerBuildingSelection(null);
		triggerSelectionChanged(null);
	}
	
	public void voidBuilding() {
		building = false;
	}
	
	public void unselect() {
		UndoableAction action = createNewSelect(null, 0, 0);
		action.performAction();
		undoEngine.storeAction(action);
	}
	
	/***
	 * Deletes the lifted data from memory.  Note: If you don't call this function
	 * any lifted data will automatically be merged with its dataContext when you
	 * start building a new selection.
	 */
	
	public boolean validateSelection() {
		if( building) finishBuildingSelection();
		
		if( scope.getSelection() == null)
			return false;
		
		return true;
	}
	
	
	private StartSelectionAction createLiftAction(LayerNode node) {
		BuiltImageData builtImage = workspace.buildData(node);
		
		// Creates a Selection Mask
		Rectangle rect = scope.getSelection().getBounds();
		BufferedImage bufferedImage = new BufferedImage( 
				rect.width, 
				rect.height, 
				BufferedImage.TYPE_INT_ARGB);
		MUtil.clearImage(bufferedImage);
		Graphics2D g2 = (Graphics2D)bufferedImage.getGraphics();
		
		// Draw the mask, clipping the bounds of drawing to only the part 
		// that the selection	intersects with the Image so that you do not 
		//  leave un-applied mask left in the image.
		rect.x = scope.getOffsetX();
		rect.y = scope.getOffsetY();
		Rectangle nodeRect = new Rectangle( 
				node.getOffsetX(), node.getOffsetY(),
				node.getLayer().getWidth(), node.getLayer().getHeight());
		Rectangle intersection = nodeRect.intersection(rect);
		
		g2.setClip(intersection.x-rect.x , intersection.y-rect.y, 
				intersection.width, intersection.height);
		
		scope.getSelection().drawSelectionMask(g2);
		
		// Copy the data inside the Selection's alphaMask to liftedData
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_IN));
		
//		g2.translate(node.getOffsetX(), node.getOffsetY());
		g2.translate(-scope.getOffsetX(), -scope.getOffsetY());

		builtImage.draw(g2);
		
		
		g2.dispose();
		
		startAction = new StartSelectionAction(bufferedImage, builtImage.handle);
		return startAction;
	}
	
	private UndoableAction createNewSelect( Selection selection, int ox, int oy) {
		UndoableAction baseAction = new SetSelectionAction( 
				new BuiltSelection(selection, ox, oy), oldSelection);

		if( scope.isLifted()) {
			List<UndoableAction> actions = new ArrayList<>(3);
			
			actions.add(new PasteSelectionAction(
					startAction, 
					workspace.builtActiveData(),
					getBuiltSelection()));
			actions.add(new EndSelectionAction(startAction));
			actions.add(baseAction);
			return undoEngine.new CompositeAction(actions, baseAction.description);
		}
		else return baseAction;
	}
	

	
	// ============= Selection-Related Undoable Actions
	
	public class MoveSelectionAction extends NullAction 
		implements StackableAction
	{
		private int dx, dy;
		
		protected MoveSelectionAction( int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
			description = "Selection Move";
		}
	
		void translate( int deltax, int deltay) {
			scope.offsetX += deltax;
			scope.offsetY += deltay;

			// Construct ImageChangeEvent and send it
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.selectionLayerChange = true;
			workspace.triggerImageRefresh(evt);
		}
		@Override
		protected void performAction() {
			translate(dx, dy);
		}
		@Override
		protected void undoAction() {
			translate(-dx, -dy);
		}
		
		@Override public void stackNewAction(UndoableAction newAction) {
			MoveSelectionAction action = (MoveSelectionAction)newAction;
			this.dx += action.dx;
			this.dy += action.dy;
		}
		@Override public boolean canStack(UndoableAction newAction) {
			return newAction instanceof MoveSelectionAction;
		}
	}
	
	/** Clears the data under the given selection. */
	public class ClearSelectionAction extends ImageAction {
		private final BuiltSelection selection;
		
		public ClearSelectionAction(BuiltSelection builtSelection, BuiltImageData builtData) {
			super(builtData);
			this.selection = builtSelection;
			this.description = "Deleted Selected Data";
		}

		@Override
		protected void performImageAction() {
			Graphics g = builtImage.checkout();
			
			Graphics2D g2 = (Graphics2D)g;
			g2.setComposite( AlphaComposite.getInstance( AlphaComposite.DST_OUT));
			selection.drawSelectionMask(g2);

			builtImage.checkin();
		}
	}
	
	public class StartSelectionAction extends NullAction {
		final CachedImage cachedData;
		StartSelectionAction( BufferedImage liftedData, ImageHandle context) {
			this.cachedData = cacheManager.cacheImage(liftedData, undoEngine);
		}
		@Override protected void performAction() {
			scope.liftedImage = cachedData;
			scope.lifted = true;
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.selectionLayerChange = true;
			workspace.triggerImageRefresh(evt);
		}
		@Override protected void undoAction() {
			scope.liftedImage = null;
			scope.lifted = false;
		}
		@Override
		protected void onAdd() {
			// Separate onAdd event rather than constructor because liftSelection
			//	creates a StartSelectionAction that doesn't necessarily get 
			//	added to the UndoEngine
			this.cachedData.reserve(this);
		}
		@Override
		protected void onDispatch() {
			cachedData.relinquish(this);
		}
	}
	public class EndSelectionAction extends NullAction {
		final CachedImage cachedData;
		EndSelectionAction( StartSelectionAction start) {
			this.cachedData = start.cachedData;
			this.cachedData.reserve(this);
		}
		@Override protected void performAction() {
			scope.liftedImage = null;
			scope.lifted = false;
		}
		@Override protected void undoAction() {
			scope.lifted = true;
			scope.liftedImage = cachedData;
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.selectionLayerChange = true;
			workspace.triggerImageRefresh(evt);
		}
		@Override
		protected void onAdd() {
			this.cachedData.reserve(this);
		}
		@Override
		protected void onDispatch() {
			cachedData.relinquish(this);
		}
	}
	public class PasteSelectionAction extends ImageAction {
		final CachedImage cachedData;
		private final BuiltSelection builtSelection;
		
		protected PasteSelectionAction(
				StartSelectionAction start, 
				BuiltImageData builtActiveData, 
				BuiltSelection builtSelection) 
		{
			super(builtActiveData);
			this.cachedData = start.cachedData;
			this.builtSelection = builtSelection;
		}
		@Override
		protected void performImageAction() {
			Graphics2D g2 = (Graphics2D)builtImage.checkout();
			g2.transform( builtSelection.getDrawFromTransform());
			g2.drawImage(cachedData.access(), 0, 0, null);
			builtImage.checkin();
		}
		
	}
	
	public class SetSelectionAction extends NullAction {
		
		private final BuiltSelection oldSelection;
		final BuiltSelection newSelection;
		
		SetSelectionAction( BuiltSelection newSelection, BuiltSelection oldSelection) 
		{
			this.oldSelection = oldSelection;
			this.newSelection = newSelection;
			description = "Selection Change";
		}
		
		@Override
		protected void performAction() {
			voidBuilding();
			
			scope.setFromBuiltSelection(newSelection);
			
			triggerBuildingSelection(null);
			triggerSelectionChanged(null);
		}

		@Override
		protected void undoAction() {
			voidBuilding();

			scope.setFromBuiltSelection(oldSelection);
			
			triggerBuildingSelection(null);
			triggerSelectionChanged(null);
		}
	}
	
	
	// ========== Selection Database
	public abstract static class Selection 
	{
		// Note: SelectionBounds is drawn in image-space (i.e. accounting
		//	for offsets), whereas SelectionMask is drawn in selection space
		//	(not accounting for offsets).
		public abstract void drawSelectionBounds( Graphics g);
		abstract void drawSelectionMask( Graphics g);
		public abstract boolean contains( int x, int y);
		public abstract Rectangle getBounds();
		abstract Rectangle clipToRect( Rectangle rect);	// returns null if the clipped Selection is empty
		
		public abstract Selection clone();
	}
	
	public class NullSelection extends Selection {
		@Override		public void drawSelectionBounds(Graphics g) {}
		@Override		void drawSelectionMask(Graphics g) {}
		@Override		public boolean contains(int x, int y) {return false;}
		@Override		public Rectangle getBounds() {return new Rectangle(0,0,0,0);}
		@Override		Rectangle clipToRect(Rectangle rect) {return null;}
		@Override		public Selection clone() { return new NullSelection();}
	}
	
	/***
	 * Simple Rectangular Selection
	 */
	public class RectSelection extends Selection {
		int width;
		int height;
		RectSelection( int width, int height) {
			this.width = width;
			this.height = height;
		}
		@Override
		public void drawSelectionBounds( Graphics g) {
			g.drawRect( 0, 0, width, height);
		}
		@Override
		void drawSelectionMask( Graphics g) {
			g.setColor( Color.black);
			g.fillRect(0, 0, width, height);
		}
		@Override
		public Rectangle getBounds() {
			return new Rectangle( 0, 0, width, height);
		}
		@Override
		public boolean contains( int x, int y) {
			return (new Rectangle( 0, 0, width, height)).contains(x,y);
		}
		
		@Override
		Rectangle clipToRect(Rectangle rect) {
			Rectangle selectionRect = new Rectangle( 0, 0, width, height);
			Rectangle intersection = rect.intersection(selectionRect);
			
			if( intersection.isEmpty())
				return null;

			width = intersection.width;
			height = intersection.height;
			
			
			return new Rectangle(intersection);
		}
		

		@Override
		public RectSelection clone()  {
			return new RectSelection( width, height);
		}
	}
	public class BuiltSelection {
		public final Selection selection;
		public final int offsetX;
		public final int offsetY;
		public BuiltSelection(Selection selection, int offsetX, int offsetY){
			this.selection = selection;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		public void drawSelectionMask(Graphics2D g2) {
			AffineTransform trans = g2.getTransform();
			
			g2.translate(offsetX, offsetY);
			selection.drawSelectionMask(g2);
			
			g2.setTransform(trans);
		}
		

		public AffineTransform getDrawFromTransform() {
			AffineTransform trans = new AffineTransform();
			trans.translate(offsetX, offsetY);
			return trans;
		}
	}

	// :::: Observers
	public static interface MSelectionEngineObserver {
		public void selectionBuilt(SelectionEvent evt);
		public void buildingSelection( SelectionEvent evt);
	}
	public static class SelectionEvent {
		Selection selection;
	}
    List<MSelectionEngineObserver> selectionObservers = new ArrayList<>();
    public void addSelectionObserver( MSelectionEngineObserver obs) { selectionObservers.add(obs);}
	public void removeSelectionObserver( MSelectionEngineObserver obs) { selectionObservers.remove(obs); }
	
    void triggerSelectionChanged(SelectionEvent evt) {
    	for( MSelectionEngineObserver obs : selectionObservers) {
    		obs.selectionBuilt(evt);
    	}
    }
    void triggerBuildingSelection(SelectionEvent evt) {
    	for( MSelectionEngineObserver obs : selectionObservers) {
    		obs.buildingSelection(evt);
    	}
    }

    
}
