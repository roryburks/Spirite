package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;

import mutil.DataCompaction.IntCompactor;
import spirite.MDebug;
import spirite.MUtil;
import spirite.MDebug.ErrorType;
import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.gl.GLUIDraw;
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
	
	// To make sure lift events are synced up, every lift
	private StartLiftAction startLiftAction = null;
	
	// Stored for the UndoEngine
	private BuiltSelection oldSelection = new BuiltSelection(null, 0, 0);
	
	// Variables relating to Building
	private boolean building = false;
	private SelectionBuilder selectionBuilder;	// Only used when building a selection
	
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
/*	public Selection getBuildingSelection() {
		return buildingSelection;
	}*/
	public void drawBuildingSelection( Graphics g) {
		if( selectionBuilder != null)
			selectionBuilder.draw(g);
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
		return scope.getOffsetX();
	}
	public int getOffsetY() {
		return scope.getOffsetY();
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

			StartLiftAction startAction = createLiftAction(node);
			
			// Construct an execute the composite action
			List<UndoableAction> actions = new ArrayList<>(3);
			actions.add( startAction);
			
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
			UndoableAction action = new EndLiftAction(startLiftAction);
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
	public void startBuildingSelection( SelectionBuilder builder, int x, int y) {
		if( builder == null) return;
		
		// !!!! For the engine to work as expected, it is important that the user
		//	only updates the selection through these building mechanisms.
		
		oldSelection = getBuiltSelection();
		
		// Start building
		building = true;
		selectionBuilder = builder;
		selectionBuilder.start(x, y);
	}
	
	public void updateBuildingSelection( int x, int y) {
		if(!building) return;
		
		selectionBuilder.update(x, y);
		
		SelectionEvent evt = new SelectionEvent();
		
		triggerBuildingSelection( evt);
		
	}

	public void finishBuildingSelection() {
		building = false;
		
		// Then Store the action and perform it
		UndoableAction action = createNewSelect( selectionBuilder.build());
		action.performAction();
		undoEngine.storeAction(  action);
		
		triggerBuildingSelection(null);
		triggerSelectionChanged(null);
	}
	
	public void cancelBuildingSelection() {
		selectionBuilder = null;
		building = false;
		triggerBuildingSelection(null);
	}
	
	// :::: Selection Setting
	public void setSelection( Selection selection, int ox, int oy) {
		undoEngine.performAndStore( createNewSelect(new BuiltSelection(selection, ox, oy)));
	}
	

	public void anchorSelection() {
		if( !scope.isLifted()) return;
		
		

		List<UndoableAction> actions = new ArrayList<>(2);
		
		actions.add(new PasteSelectionAction(
				startLiftAction, 
				workspace.buildActiveData(),
				getBuiltSelection()));
		actions.add(new EndLiftAction(startLiftAction));
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Anchored Selection"));
	}
	public void imageToSelection( BufferedImage bi, int ox, int oy) {
		if( bi == null) return;
		
		List<UndoableAction> actions = new ArrayList<>(2);
		actions.add(createNewSelect(new BuiltSelection(new RectSelection(bi.getWidth(), bi.getHeight()), ox, oy)));
		startLiftAction = new StartLiftAction(bi);
		actions.add( startLiftAction);
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Pasted Image to Layer"));
	}
	
	
	public void unselect() {
		UndoableAction action = createNewSelect( new BuiltSelection(null, 0, 0));
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
	
	
	private StartLiftAction createLiftAction(LayerNode node) {
		BuiltSelection mask = getBuiltSelection();
		BuiltImageData builtImage = workspace.buildData(node);
		
		startLiftAction = new StartLiftAction(mask.liftSelectionFromData(builtImage));
		return startLiftAction;
	}
	
	private UndoableAction createNewSelect( BuiltSelection selection) {
		UndoableAction baseAction = new SetSelectionAction( selection, oldSelection);

		if( scope.isLifted()) {
			List<UndoableAction> actions = new ArrayList<>(3);
			
			actions.add(new PasteSelectionAction(
					startLiftAction, 
					workspace.buildActiveData(),
					getBuiltSelection()));
			actions.add(new EndLiftAction(startLiftAction));
			actions.add(baseAction);
			return undoEngine.new CompositeAction(actions, baseAction.description);
		}
		else return baseAction;
	}
	
	void voidBuilding() {
		building = false;
	}
	

	
	// ============= Selection-Related Undoable Actions
	/** Since actions on selection care very much about what node is being selected
	 * make sure all logical selection actions remember and reset that node.*/
	public abstract class SelectionAction extends NullAction 
	{
		private final Node selectedNode;
		SelectionAction( Node selected) {selectedNode = selected;}
		@Override protected void performAction() {
			workspace.setSelectedNode(selectedNode);
		}
		@Override protected void undoAction() {
			workspace.setSelectedNode(selectedNode);
		}
	}
	
	public class MoveSelectionAction extends SelectionAction 
		implements StackableAction
	{
		private int dx, dy;
		
		protected MoveSelectionAction( int dx, int dy) {
			super( workspace.getSelectedNode());
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
			super.performAction();
			translate(dx, dy);
		}
		@Override
		protected void undoAction() {
			super.undoAction();
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
	
	public class StartLiftAction extends SelectionAction {
		final CachedImage cachedData;
		StartLiftAction( BufferedImage liftedData) {
			super( workspace.getSelectedNode());
			this.cachedData = cacheManager.cacheImage(liftedData, undoEngine);
		}
		@Override protected void performAction() {
			super.performAction();
			scope.liftedImage = cachedData;
			scope.lifted = true;
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.selectionLayerChange = true;
			workspace.triggerImageRefresh(evt);
		}
		@Override protected void undoAction() {
			super.undoAction();
			scope.liftedImage = null;
			scope.lifted = false;
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.selectionLayerChange = true;
			workspace.triggerImageRefresh(evt);
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
	
	/** Ends the Lifted Data action, clearing any data that was lifted,
	 * but doesn't change the actual selection.*/
	public class EndLiftAction extends SelectionAction {
		final CachedImage cachedData;
		private static final String DESC = "Cleared Lifted Image";
		EndLiftAction( StartLiftAction start) {
			super( workspace.getSelectedNode());
			this.cachedData = start.cachedData;
			this.cachedData.reserve(this);
			description = DESC;
		}
		@Override protected void performAction() {
			super.performAction();
			scope.liftedImage = null;
			scope.lifted = false;
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.selectionLayerChange = true;
			workspace.triggerImageRefresh(evt);
		}
		@Override protected void undoAction() {
			super.undoAction();
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
				StartLiftAction start, 
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
	
	public class SetSelectionAction extends SelectionAction {
		
		private final BuiltSelection oldSelection;
		final BuiltSelection newSelection;
		
		SetSelectionAction( BuiltSelection newSelection, BuiltSelection oldSelection) 
		{
			super( workspace.getSelectedNode());
			this.oldSelection = oldSelection;
			this.newSelection = newSelection;
			description = "Selection Change";
		}
		
		@Override
		protected void performAction() {
			super.performAction();
			voidBuilding();
			
			scope.setFromBuiltSelection(newSelection);
			
			triggerBuildingSelection(null);
			triggerSelectionChanged(null);
		}

		@Override
		protected void undoAction() {
			super.undoAction();
			voidBuilding();

			scope.setFromBuiltSelection(oldSelection);
			
			triggerBuildingSelection(null);
			triggerSelectionChanged(null);
		}
	}	
	
	// ========== Selection Database
	public abstract static class SelectionBuilder
	{
		protected abstract void start( int x, int y);
		protected abstract void update( int x, int y);
		protected abstract BuiltSelection build();
		protected abstract void draw( Graphics g);
	}
	
	//
	public abstract static class Selection 
	{
		// Note: SelectionBounds is drawn in image-space (i.e. accounting
		//	for offsets), whereas SelectionMask is drawn in selection space
		//	(not accounting for offsets).
		public abstract void drawSelectionBounds( Graphics g);
		public abstract void drawSelectionMask( Graphics g);
		public abstract boolean contains( int x, int y);
		public abstract Dimension getDimension();
		
		public abstract Selection clone();
	}
	
	public static class NullSelection extends Selection {
		@Override		public void drawSelectionBounds(Graphics g) {}
		@Override		public void drawSelectionMask(Graphics g) {}
		@Override		public boolean contains(int x, int y) {return false;}
		@Override		public Dimension getDimension() {return new Dimension(0,0);}
		@Override		public Selection clone() { return new NullSelection();}
	}
	
	public class RectSelectionBuilder extends SelectionBuilder {
		private int startX;
		private int startY;
		private int currentX;
		private int currentY;
		
		@Override
		protected void start(int x, int y) {
			startX = currentX = x;
			startY = currentY = y;
		}

		@Override
		protected void update(int x, int y) {
			currentX = x;
			currentY = y;
		}
		@Override
		protected BuiltSelection build() {
			Rectangle buildingRect = new Rectangle( 
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
			
			Rectangle intersection = buildingRect.intersection(
					new Rectangle(0,0, workspace.getWidth(), workspace.getHeight()));
			
			if( intersection == null || intersection.isEmpty())
				return new BuiltSelection( null, 0, 0);
			
			return new BuiltSelection( new RectSelection(
					intersection.width, intersection.height), intersection.x, intersection.y);
		}
		@Override
		protected void draw(Graphics g) {
			g.drawRect(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
		}
	}
	
	/***
	 * Simple Rectangular Selection
	 */
	public static class RectSelection extends Selection {
		int width;
		int height;
		public RectSelection( int width, int height) {
			this.width = width;
			this.height = height;
		}
		@Override
		public void drawSelectionBounds( Graphics g) {
			g.drawRect( 0, 0, width, height);
		}
		@Override
		public void drawSelectionMask( Graphics g) {
			g.setColor( Color.black);
			g.fillRect(0, 0, width, height);
		}
		@Override
		public Dimension getDimension() {
			return new Dimension( width, height);
		}
		@Override
		public boolean contains( int x, int y) {
			return (new Rectangle( 0, 0, width, height)).contains(x,y);
		}

		@Override
		public RectSelection clone()  {
			return new RectSelection( width, height);
		}
	}
	
	public class OvalSelectionBuilder extends SelectionBuilder {
		private int startX;
		private int startY;
		private int currentX;
		private int currentY;
		
		@Override
		protected void start(int x, int y) {
			startX = currentX = x;
			startY = currentY = y;
		}
	
		@Override
		protected void update(int x, int y) {
			currentX = x;
			currentY = y;
		}
		@Override
		protected BuiltSelection build() {
			Rectangle buildingRect = new Rectangle( 
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
			
			// TODO
//			Rectangle intersection = buildingRect.intersection(
//					new Rectangle(0,0, workspace.getWidth(), workspace.getHeight()));
			
			if( buildingRect == null || buildingRect.isEmpty())
				return new BuiltSelection( null, 0, 0);
			
			return new BuiltSelection( new OvalSelection(
					buildingRect.width, buildingRect.height), buildingRect.x, buildingRect.y);
		}
		@Override
		protected void draw(Graphics g) {
			g.drawOval(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
		}
	}
	public static class OvalSelection extends Selection {
		private int width;
		private int height;
		public OvalSelection( int width, int height) {
			this.width = width;
			this.height = height;
		}
		@Override
		public void drawSelectionBounds( Graphics g) {
			g.drawOval( 0, 0, width, height);
		}
		@Override
		public void drawSelectionMask( Graphics g) {
			g.setColor( Color.black);
			g.fillOval(0, 0, width, height);
		}
		@Override
		public Dimension getDimension() {
			return new Dimension( width, height);
		}
		@Override
		public boolean contains( int x, int y) {
			return (new Rectangle( 0, 0, width, height)).contains(x,y);
		}
		@Override
		public OvalSelection clone()  {
			return new OvalSelection( width, height);
		}
	}
	public class FreeformSelectionBuilder extends SelectionBuilder {
		IntCompactor compactor = new IntCompactor();
		@Override
		protected void start(int x, int y) {
			compactor.add(x);
			compactor.add(y);
		}

		@Override
		protected void update(int x, int y) {
			compactor.add(x);
			compactor.add(y);
		}
		@Override
		protected BuiltSelection build() {
			BufferedImage bi = new BufferedImage( workspace.getWidth(), workspace.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
			
			Polygon pg = new Polygon();
			for( int i=0; i<compactor.size(); i += 2) {
				pg.addPoint(compactor.get(i), compactor.get(i+1));
			}
			Graphics g = bi.getGraphics();
			g.setColor(Color.BLACK);
			g.fillPolygon(pg);
			g.dispose();
			
			try {
				Rectangle bounds = MUtil.findContentBounds(bi, 2, true);
				if( bounds == null || bounds.isEmpty())
					return new BuiltSelection( null, 0, 0);
				
				BufferedImage bi2 = bi;
				bi = new BufferedImage( bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB_PRE);
				g = bi.getGraphics();
				g.drawImage(bi2, -bounds.x, -bounds.y, null);
				g.dispose();
				return new BuiltSelection(new ImageSelection(bi), bounds.x, bounds.y);
			} catch (UnsupportedDataTypeException e) {
				MDebug.handleError(ErrorType.STRUCTURAL, e, e.getMessage());
			}
			
			
			return new BuiltSelection(new ImageSelection(bi), 0, 0);
		}

		@Override
		protected void draw(Graphics g) {
			System.out.println(compactor.size());
			Graphics2D g2 = (Graphics2D)g;

			int ox = compactor.get(0);
			int oy = compactor.get(1);
			int nx, ny;
			for( int i=2; i<compactor.size(); i+=2) {
				nx = compactor.get(i);
				ny = compactor.get(i+1);
				g.drawLine(ox, oy, nx, ny);
				ox = nx;
				oy = ny;
			}
		}
		public Point getStart() {
			return new Point( compactor.get(0), compactor.get(1));
		}
		public Point getEnd() {
			int s = compactor.size();
			return new Point( compactor.get(s-2), compactor.get(s-1));
		}
		
	}
	
	public class ImageSelection extends Selection {
		private final BufferedImage bi;
		
		/** NOTE: To properly draw the border, the supplied buffered image should
		 * be cropped with a 1-pixel border (if it's cropped at all).
		 */
		public ImageSelection(BufferedImage bi) {
			this.bi = bi;
		}
		@Override
		public void drawSelectionBounds(Graphics g) {
			g.drawImage(GLUIDraw.drawBounds(bi), 0, 0, null);
		}
		@Override
		public void drawSelectionMask(Graphics g) {
			g.drawImage(bi, 0, 0, null);
		}
		@Override
		public boolean contains(int x, int y) {
			if( x < 0 || x > bi.getWidth() || y < 0 || y > bi.getHeight())
				return false;
			
			return ((bi.getRGB(x, y) >>> 24)!= 0);
		}
		@Override
		public Dimension getDimension() {
			return new Dimension( bi.getWidth(), bi.getHeight());
		}
		@Override
		public Selection clone() {
			return new ImageSelection(MUtil.deepCopy(bi));
		}
		
	}
	
	/** Helper Class to reduce duplicate code. */
	private interface LiftScheme {
		void draw(Graphics g);
		Rectangle getBounds();
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
		
		private BufferedImage liftSelection( LiftScheme liftScheme) {
			Rectangle selectionRect = new Rectangle(selection.getDimension());
			selectionRect.x = this.offsetX;
			selectionRect.y = this.offsetY;
			
			BufferedImage bi = new BufferedImage( 
					selectionRect.width, selectionRect.height, BufferedImage.TYPE_INT_ARGB_PRE);
			Graphics2D g2 = (Graphics2D)bi.getGraphics();

			// Draw the mask, clipping the bounds of drawing to only the part 
			// that the selection	intersects with the Image so that you do not 
			//  leave un-applied mask left in the image.
			Rectangle dataRect = liftScheme.getBounds();
			Rectangle intersection = dataRect.intersection(selectionRect);
			
			g2.setClip(intersection.x - selectionRect.x, intersection.y - selectionRect.y, 
					intersection.width, intersection.height);
			selection.drawSelectionMask(g2);	// Note: Untransformed
			

			// Copy the data inside the Selection's alphaMask to liftedData
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_IN));

			g2.translate(-this.offsetX, -this.offsetY);
			
			liftScheme.draw(g2);
			g2.dispose();
			
			return bi;
		}
		public BufferedImage liftSelectionFromImage( 
				BufferedImage img, int offsetX, int offsetY)
		{
			return liftSelection( new LiftScheme() {
				@Override
				public Rectangle getBounds() {
					return new Rectangle( offsetX, offsetY, img.getWidth(), img.getHeight());
				}
				
				@Override
				public void draw(Graphics g) {
					g.drawImage(img, 0, 0, null);
				}
			});
		}
		public BufferedImage liftSelectionFromData( BuiltImageData data) {
			return liftSelection(new LiftScheme() {
				@Override
				public Rectangle getBounds() {
					return data.getBounds();
				}
				
				@Override
				public void draw(Graphics g) {
					data.draw(g);
				}
			});
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
