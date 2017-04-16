package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;

import mutil.DataCompaction.IntCompactor;
import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;
import spirite.graphics.GraphicsContext;
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
	private final ImageWorkspace workspace;
	private final UndoEngine undoEngine;
	
	/** Cached ImageChangeEvent that represents a SelectionChange. */
	private final ImageChangeEvent selChangeEvt = new ImageChangeEvent();
	
	// Variables related to Selection state
	private BuiltSelection built = new BuiltSelection(null,0,0);
	private boolean lifted = false;
	protected BufferedImage liftedImage = null;
	
	
	
	// Variables relating to Building
	private boolean building = false;
	private SelectionBuilder selectionBuilder;	// Only used when building a selection
	public enum BuildMode {
		DEFAULT, ADD, SUBTRACT, INTERSECTION
	};
	private BuildMode buildMode;
	
	// Variables relating to Transforming
	private boolean proposingTransform = false;
	AffineTransform proposedTransform = null;

	
	SelectionEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();

		selChangeEvt.workspace = workspace;
		selChangeEvt.selectionLayerChange = true;
		workspace.triggerImageRefresh(selChangeEvt);
	}

	// :::: Get/Set
	public boolean isLifted() {
		return lifted;
	}
	
	public boolean isBuilding() {
		return building;
	}
	public void drawBuildingSelection( GraphicsContext gc) {
		if( selectionBuilder != null)
			selectionBuilder.draw(gc);
	}
	
	/** Returns a BuiltSelection which incorporates all relevant offset data
	 * such that Workspace space is converted into Selection space on the
	 * node it's selecting.
	 * 
	 * (Consider incorperating SelectedNode into this?)
	 */
	public BuiltSelection getBuiltSelection() {	return built;}
	private void setBuiltSelection( BuiltSelection sel) {
		built = (sel == null) ? new BuiltSelection(null, 0, 0) : sel;
		
		proposingTransform = false;
	}
	
	public Selection getSelection() {return built.selection;}
	public BufferedImage getLiftedImage() {return liftedImage;}

	public AffineTransform getDrawFromTransform() {
		AffineTransform trans = new AffineTransform();
		if( proposingTransform) {
			Dimension d = built.selection.getDimension();
			trans.translate(built.offsetX+d.width/2, built.offsetY+d.height/2);
			trans.concatenate(proposedTransform);
			trans.translate(-d.width/2, -d.height/2);
		}
		else
			trans.translate(built.offsetX, built.offsetY);
		return trans;
	}
	
	public int getOffsetX() {return built.offsetX;}
	public int getOffsetY() {return built.offsetY;}
	public void setOffset( int x, int y) {
		int dx = x - built.offsetX;
		int dy = y - built.offsetY;

		if( !lifted) {
			if( !validateSelection())
				return;
			
			Node snode = workspace.getSelectedNode();
			if( !(snode instanceof LayerNode))
				return;
			LayerNode node = (LayerNode)snode;
			
			// Construct an execute the composite action
			List<UndoableAction> actions = new ArrayList<>(3);
			actions.add( createLiftAction(node));
			
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
	
	
	// ==================
	// ==== Data Lifting
	public void liftData() {
//		if( !lifted) {
			if( !validateSelection())
				return;
			
			Node snode = workspace.getSelectedNode();
			if( !(snode instanceof LayerNode))
				return;
			LayerNode node = (LayerNode)snode;
			
			// Construct an execute the composite action
			List<UndoableAction> actions = new ArrayList<>(3);
			actions.add( createLiftAction(node));
			actions.add(new ClearSelectionAction(
					getBuiltSelection(),
					workspace.buildData(node)));
					
			
			UndoableAction action = undoEngine.new StackableCompositeAction(actions, "Lift Data");
			action.performAction();
			undoEngine.storeAction(action);
//		}
	}
	
	
	/** Clears the lifted data if it exists */
	public boolean attemptClearSelection() {
		if( lifted) {
			// If you have lifted data, it clears the lifted data without changing the
			//	 fact that it is lifted
			UndoableAction action = new SetLiftedAction(null);
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

	/** Anchors the current floating selection into the active layer*/
	public void anchorSelection() {
		if( !lifted) return;
		
		List<UndoableAction> actions = new ArrayList<>(2);
		
		actions.add(new PasteSelectionAction(
				liftedImage, 
				workspace.buildActiveData(),
				getBuiltSelection()));
		actions.add(new SetLiftedAction(null));
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Anchored Selection"));
	}
	
	// ==================
	// ==== Transform proposing
	public void proposeTransform( AffineTransform trans) {
		this.proposingTransform = true;
		this.proposedTransform = trans;
		workspace.triggerImageRefresh(selChangeEvt);
	}
	public void stopProposintTransform() {
		if( proposingTransform) {
			proposingTransform = false;
			workspace.triggerImageRefresh(selChangeEvt);
		}
		
	}
	public void applyProposedTransform() {
		if( proposingTransform) {
			transformSelection(proposedTransform);
			proposingTransform = false;
		}
	}
	
	// ===================
	// ==== Selection Building
	// ===================
	public void startBuildingSelection( SelectionBuilder builder, int x, int y, BuildMode mode) {
		if( builder == null) return;
		
		// Start building
		building = true;
		selectionBuilder = builder;
		buildMode = mode;
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
		
		switch( buildMode) {
		case DEFAULT:
			setSelection(selectionBuilder.build());
			break;
		case ADD:
			setSelection(combineSelection(getBuiltSelection(),selectionBuilder.build()));
			break;
		case SUBTRACT:
			setSelection(subtractSelection(getBuiltSelection(),selectionBuilder.build()));
			break;
		case INTERSECTION:
			setSelection(intersectSelection(getBuiltSelection(),selectionBuilder.build()));
			break;
		}
	}
	
	public void cancelBuildingSelection() {
		selectionBuilder = null;
		building = false;
		triggerBuildingSelection(null);
	}
	
	// ==============
	// ==== Selection modification
	// ==============
	/** Sets the selection to a new selection, triggering all observer actions,
	 * lifting/anchoring data as needed, and inserting it into the undo engine. */
	public void setSelection( BuiltSelection selection) {
		undoEngine.performAndStore( createNewSelectAction(selection));
		triggerBuildingSelection(null);
		triggerSelectionChanged(null);
	}
		
	public BuiltSelection combineSelection( BuiltSelection sel1, BuiltSelection sel2) {
		BufferedImage bi = new BufferedImage(
				workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
		
		Graphics g = bi.getGraphics();
		if( sel1 != null)
			sel1.drawSelectionMask(g);
		if( sel2 != null)
			sel2.drawSelectionMask(g);
		g.dispose();
		
		return new BuiltSelection( bi);
	}
	public BuiltSelection subtractSelection( BuiltSelection sel1, BuiltSelection sel2){

		BufferedImage bi = new BufferedImage(
				workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
		
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		if( sel1 != null)
			sel1.drawSelectionMask(g2);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
		if( sel2 != null)
			sel2.drawSelectionMask(g2);
		g2.dispose();

		return new BuiltSelection( bi);
	}
	public BuiltSelection intersectSelection( BuiltSelection sel1, BuiltSelection sel2){
		BufferedImage bi = new BufferedImage(
				workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
		BufferedImage bi2 = new BufferedImage(
				workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
		
		if( sel1 == null || sel2 == null) return new BuiltSelection( null,0,0);
		
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		sel1.drawSelectionMask(g2);
		g2.dispose();
		

		g2 = (Graphics2D)bi2.getGraphics();
		sel2.drawSelectionMask(g2);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
		g2.drawImage( bi, 0, 0, null);
		g2.dispose();

		return new BuiltSelection( bi2);
	}
	public BuiltSelection invertSelection( BuiltSelection sel) {
		BufferedImage bi = new BufferedImage(
				workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
		
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, workspace.getWidth(), workspace.getHeight());
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
		if( sel != null)
			sel.drawSelectionMask(g2);
		g2.dispose();

		return new BuiltSelection( bi);
	}
	public void transformSelection( AffineTransform trans) {
		BufferedImage bi = new BufferedImage(
				workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		
		Dimension d = built.selection.getDimension();
		g2.translate(built.offsetX+d.width/2, built.offsetY+d.height/2);
		g2.transform(trans);
		g2.translate(-d.width/2, -d.height/2);
		built.selection.drawSelectionMask(g2);
		g2.dispose();
		BuiltSelection sel = new BuiltSelection(bi);
		
		List<UndoableAction> actions = new ArrayList<>(2); 
		actions.add( new SetSelectionAction(sel));
		
		if( lifted) {
			bi = new BufferedImage(
					workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
			g2 = (Graphics2D)bi.getGraphics();
			g2.translate(built.offsetX+d.width/2, built.offsetY+d.height/2);
			g2.transform(trans);
			g2.translate(-d.width/2, -d.height/2);
			g2.drawImage(liftedImage, 0, 0, null);
			g2.dispose();
			
			Dimension afterDim = sel.selection.getDimension();
			
			
			BufferedImage afterLift = new BufferedImage(
					afterDim.width, afterDim.height,  Globals.BI_FORMAT);
			g2 = (Graphics2D)afterLift.getGraphics();
			g2.drawImage(bi, -sel.offsetX, -sel.offsetY, null);
			g2.dispose();
			
			actions.add( new SetLiftedAction(afterLift));
		}
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Transform Lifted Selection"));
	}
	

	
	
	public void imageToSelection( BufferedImage bi, int ox, int oy) {
		if( bi == null) return;
		
		
		
		List<UndoableAction> actions = new ArrayList<>(2);
		actions.add(createNewSelectAction(buildRectSelection(
				new Rectangle( ox, oy, bi.getWidth(), bi.getHeight()))));
		actions.add( new SetLiftedAction(bi));
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Pasted Image to Layer"));
	}
	
	public BuiltSelection buildRectSelection( Rectangle rect) {
		RectSelectionBuilder rsb = new RectSelectionBuilder();
		rsb.start(rect.x, rect.y);
		rsb.update(rect.x+rect.width, rect.y+rect.height);
		return rsb.build();
	}
	
	
	public void unselect() {
		UndoableAction action = createNewSelectAction( new BuiltSelection(null, 0, 0));
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
		
		if( built == null)
			return false;
		
		return true;
	}
	
	
	private SetLiftedAction createLiftAction(LayerNode node) {
		BuiltSelection mask = getBuiltSelection();
		BuiltImageData builtImage = workspace.buildData(node);
		
		return new SetLiftedAction(mask.liftSelectionFromData(builtImage));
	}
	
	public UndoableAction createNewSelectAction( BuiltSelection selection) {
		if( selection == null) 
			selection = new BuiltSelection(null, 0, 0);
		UndoableAction baseAction = new SetSelectionAction( selection);

		if( lifted) {
			List<UndoableAction> actions = new ArrayList<>(3);
			
			actions.add(new PasteSelectionAction(
					liftedImage, 
					workspace.buildActiveData(),
					getBuiltSelection()));
			actions.add(new SetLiftedAction(null));
			actions.add(baseAction);
			return undoEngine.new CompositeAction(actions, baseAction.description);
		}
		else return baseAction;
	}
	
	private void voidBuilding() {
		building = false;
	}
	
	// ========================
	// ==== Selection-Related Undoable Actions
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
	
	
	public class PasteSelectionAction extends ImageAction {
		final BufferedImage liftedImage;
		private final BuiltSelection builtSelection;
		
		protected PasteSelectionAction(
				BufferedImage liftedImage, 
				BuiltImageData builtActiveData, 
				BuiltSelection builtSelection) 
		{
			super(builtActiveData);
			this.liftedImage = liftedImage;
			this.builtSelection = builtSelection;
		}
		@Override
		protected void performImageAction() {
			Graphics2D g2 = (Graphics2D)builtImage.checkout();
			g2.drawImage(liftedImage, builtSelection.offsetX, builtSelection.offsetY, null);
			builtImage.checkin();
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
			setBuiltSelection(new BuiltSelection( built.selection, 
					built.offsetX+deltax,built.offsetY+deltay));

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
	public class SetSelectionAction extends SelectionAction {
		
		private final BuiltSelection oldSelection;
		final BuiltSelection newSelection;
		
		SetSelectionAction( BuiltSelection newSelection) 
		{
			super( workspace.getSelectedNode());
			this.oldSelection = getBuiltSelection();
			this.newSelection = newSelection;
			description = "Selection Change";
		}
		
		@Override
		protected void performAction() {
			super.performAction();
			voidBuilding();

			setBuiltSelection(newSelection);
			
			triggerBuildingSelection(null);
			triggerSelectionChanged(null);
		}

		@Override
		protected void undoAction() {
			super.undoAction();
			voidBuilding();

			setBuiltSelection(oldSelection);
			
			triggerBuildingSelection(null);
			triggerSelectionChanged(null);
		}
	}
	public class SetLiftedAction extends SelectionAction {
		private final BufferedImage newLifted, oldLifted;
		SetLiftedAction( BufferedImage newLifted) 
		{
			super( workspace.getSelectedNode());
			this.newLifted = newLifted;
			this.oldLifted = liftedImage;
		}
		@Override
		protected void performAction() {
			super.performAction();
			liftedImage = newLifted;
			lifted = (newLifted != null);
			workspace.triggerImageRefresh(selChangeEvt);
		}

		@Override
		protected void undoAction() {
			super.undoAction();
			liftedImage = oldLifted;
			lifted = (oldLifted != null);
			workspace.triggerImageRefresh(selChangeEvt);
		}
	}
	
	// =============
	// ==== Selection Classes 
	/** Selection is a container for the Selection Mask, without any offsets
	 * or other transforms built into it.*/
	public static class Selection 
	{
		private final BufferedImage bi;
		static int c = 0;
		
		/** NOTE: To properly draw the border, the supplied buffered image should
		 * be cropped with a 1-pixel border (if it's cropped at all).
		 */
		Selection(BufferedImage bi) {
			this.bi = bi;
		}
		
		public void drawSelectionBounds(GraphicsContext context) {
			context.drawBounds(bi, c);
		}
		public void drawSelectionMask(Graphics g) {
			g.drawImage(bi, 0, 0, null);
		}
		public boolean contains(int x, int y) {
			if( x < 0 || x >= bi.getWidth() || y < 0 || y >= bi.getHeight())
				return false;
			
			return ((bi.getRGB(x, y) >>> 24)!= 0);
		}
		public Dimension getDimension() {
			return new Dimension( bi.getWidth(), bi.getHeight());
		}
		public Selection clone() {
			return new Selection(MUtil.deepCopy(bi));
		}
	}

	/** BuiltSelection represents a complete Selection, including offsets and 
	 * other transforms.*/
	public static class BuiltSelection {
		public final Selection selection;
		public final int offsetX;
		public final int offsetY;

		public BuiltSelection( BufferedImage bi) {
			Rectangle bounds = null;
			try {
				bounds = MUtil.findContentBounds(bi, 2, true);
			} catch (UnsupportedDataTypeException e) {
				MDebug.handleError(ErrorType.STRUCTURAL, e, e.getMessage());
			}
			if( bounds == null || bounds.isEmpty()) {
				selection = null;
				offsetX = 0;
				offsetY = 0;
			}
			else {
				BufferedImage bi2 = bi;
				bi = new BufferedImage( 
						bounds.width, bounds.height, Globals.BI_FORMAT);
				Graphics g = bi.getGraphics();
				g.drawImage(bi2, -bounds.x, -bounds.y, null);
				g.dispose();
				
				selection = new Selection(bi);
				offsetX = bounds.x;
				offsetY = bounds.y;
			}
		}
		
		public BuiltSelection(Selection selection, int offsetX, int offsetY){
			this.selection = selection;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		public void drawSelectionMask(Graphics g) {
			if( selection == null) return;
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform trans = g2.getTransform();
			
			g.translate(offsetX, offsetY);
			selection.drawSelectionMask(g2);
			
			g2.setTransform(trans);
		}
		
		private BufferedImage liftSelection( LiftScheme liftScheme) {
			Rectangle selectionRect = new Rectangle(selection.getDimension());
			selectionRect.x = this.offsetX;
			selectionRect.y = this.offsetY;
			
			BufferedImage bi = new BufferedImage( 
					selectionRect.width, selectionRect.height, Globals.BI_FORMAT);
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
		
		/** Uses the BuiltSelection to lift the selected portion of the given
		 * BufferedImage and put it in a new BufferedImage.*/
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

		/** Uses the BuiltSelection to lift the selected portion of the given
		 * BuiltImageData and put it in a new BufferedImage.*/
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
	}
	/** Helper Class to reduce duplicate code. */
	private interface LiftScheme {
		void draw(Graphics g);
		Rectangle getBounds();
	}
	
	// ======================
	// ==== Selection Builders
	public abstract static class SelectionBuilder
	{
		protected abstract void start( int x, int y);
		protected abstract void update( int x, int y);
		protected abstract BuiltSelection build();
		protected abstract void draw( GraphicsContext gc);
	}
	
	/** Builds a Rectangular Selection*/
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
			BufferedImage bi = new BufferedImage( 
					workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
			Graphics g = bi.getGraphics();
			g.fillRect(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
			g.dispose();
			
			return new BuiltSelection( bi);
		}
		@Override
		protected void draw(GraphicsContext g) {
			g.drawRect(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
		}
	}

	/** Builds an Elliptical Selection*/
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
			BufferedImage bi = new BufferedImage( 
					workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
			Graphics g = bi.getGraphics();
			g.fillOval(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
			g.dispose();
			
			return new BuiltSelection( bi);
		}
		@Override
		protected void draw(GraphicsContext g) {
			g.drawOval(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
		}
	}
	
	/** Builds a polygonal selection using a feed of points. */
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
			BufferedImage bi = new BufferedImage( workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
			
			Polygon pg = new Polygon();
			for( int i=0; i<compactor.size(); i += 2) {
				pg.addPoint(compactor.get(i), compactor.get(i+1));
			}
			Graphics g = bi.getGraphics();
			g.setColor(Color.WHITE);
			g.fillPolygon(pg);
			g.dispose();
			
			return new BuiltSelection(bi);
		}

		@Override
		protected void draw(GraphicsContext g) {
			int ox = compactor.get(0);
			int oy = compactor.get(1);
			int nx, ny;
			for( int i=2; i<compactor.size(); i+=2) {
				nx = compactor.get(i);
				ny = compactor.get(i+1);
//				g.drawLine(ox, oy, nx, ny);
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
	

	// ============
	// ==== Observers
	
	/** SelectionEngineObservers trigger as the selection that's being built
	 * changes and when the Built Selection has changed. */
	public static interface MSelectionEngineObserver {
		public void selectionBuilt(SelectionEvent evt);
		public void buildingSelection( SelectionEvent evt);
	}
	public static class SelectionEvent {
		Selection selection;
	}
    List<WeakReference<MSelectionEngineObserver>> selectionObservers = new ArrayList<>();
    public void addSelectionObserver( MSelectionEngineObserver obs) { 
    	selectionObservers.add(new WeakReference<MSelectionEngineObserver>(obs));
    }
	public void removeSelectionObserver( MSelectionEngineObserver obs) { 
		Iterator<WeakReference<MSelectionEngineObserver>> it = selectionObservers.iterator();
		while( it.hasNext()) {
			MSelectionEngineObserver other = it.next().get();
			if( other == null || other.equals(obs))
				it.remove();
		}
	}
	
    void triggerSelectionChanged(SelectionEvent evt) {
		Iterator<WeakReference<MSelectionEngineObserver>> it = selectionObservers.iterator();
		while( it.hasNext()) {
			MSelectionEngineObserver obs = it.next().get();
			if( obs == null) it.remove();
			else obs.selectionBuilt(evt);
		}
    }
    void triggerBuildingSelection(SelectionEvent evt) {
		Iterator<WeakReference<MSelectionEngineObserver>> it = selectionObservers.iterator();
		while( it.hasNext()) {
			MSelectionEngineObserver obs = it.next().get();
			if( obs == null) it.remove();
			else obs.buildingSelection(evt);
		}
    }

    
}
