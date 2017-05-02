package spirite.base.image_data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.awt.AWTContext;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuiltImageData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.util.Colors;
import spirite.base.util.DataCompaction.IntCompactor;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

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
	protected RawImage liftedImage = null;
	
	
	
	// Variables relating to Building
	private boolean building = false;
	private SelectionBuilder selectionBuilder;	// Only used when building a selection
	public enum BuildMode {
		DEFAULT, ADD, SUBTRACT, INTERSECTION
	};
	private BuildMode buildMode;
	
	// Variables relating to Transforming
	private boolean proposingTransform = false;
	MatTrans proposedTransform = null;

	
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
	public RawImage getLiftedImage() {return liftedImage;}

	public MatTrans getDrawFromTransform() {
		MatTrans trans = new MatTrans();
		if( proposingTransform) {
			Vec2i d = built.selection.getDimension();
			trans.translate(built.offsetX+d.x/2, built.offsetY+d.y/2);
			trans.concatenate(proposedTransform);
			trans.translate(-d.x/2, -d.y/2);
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
	public void proposeTransform( MatTrans trans) {
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
		RawImage img = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
		
		GraphicsContext gc = img.getGraphics();
		if( sel1 != null)
			sel1.drawSelectionMask(gc);
		if( sel2 != null)
			sel2.drawSelectionMask(gc);
//		g.dispose();
		
		return new BuiltSelection( img);
	}
	public BuiltSelection subtractSelection( BuiltSelection sel1, BuiltSelection sel2){
		RawImage img = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
		
		GraphicsContext gc = img.getGraphics();
		if( sel1 != null)
			sel1.drawSelectionMask(gc);
		gc.setComposite( Composite.DST_OUT, 1.0f);
		if( sel2 != null)
			sel2.drawSelectionMask(gc);
//		gc.dispose();

		return new BuiltSelection( img);
	}
	public BuiltSelection intersectSelection( BuiltSelection sel1, BuiltSelection sel2){
		RawImage img1 = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
		RawImage img2 = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
		
		if( sel1 == null || sel2 == null) return new BuiltSelection( null,0,0);
		
		GraphicsContext gc = img1.getGraphics();
		sel1.drawSelectionMask( gc);
//		g2.dispose();
		

		gc = img2.getGraphics();
		sel2.drawSelectionMask(gc);
		gc.setComposite(Composite.DST_IN, 1.0f);
		gc.drawImage( img1, 0, 0);
//		g2.dispose();

		return new BuiltSelection( img2);
	}
	public BuiltSelection invertSelection( BuiltSelection sel) {
		RawImage img = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
		
		GraphicsContext gc = img.getGraphics();
		gc.setColor(Colors.WHITE);
		gc.fillRect(0, 0, workspace.getWidth(), workspace.getHeight());
		gc.setComposite(Composite.DST_OUT, 1.0f);
		if( sel != null)
			sel.drawSelectionMask(gc);
//		g2.dispose();

		return new BuiltSelection( img);
	}
	public void transformSelection( MatTrans trans) {
		RawImage img = HybridHelper.createImage(workspace.getWidth(), workspace.getHeight());
		
		GraphicsContext gc = img.getGraphics();
		
		Vec2i d = built.selection.getDimension();
		gc.translate(built.offsetX+d.x/2, built.offsetY+d.y/2);
		gc.transform(trans);
		gc.translate(-d.x/2, -d.y/2);
		built.selection.drawSelectionMask(gc);
//		g2.dispose();
		BuiltSelection sel = new BuiltSelection( img);
		
		List<UndoableAction> actions = new ArrayList<>(2); 
		actions.add( new SetSelectionAction(sel));
		
		if( lifted) {
			img = HybridHelper.createImage(workspace.getWidth(), workspace.getHeight());
			gc = img.getGraphics();
			gc.translate(built.offsetX+d.x/2, built.offsetY+d.y/2);
			gc.transform(trans);
			gc.translate(-d.x/2, -d.y/2);
			gc.drawImage( liftedImage, 0, 0);
//			g2.dispose();
			
			Vec2i afterDim = sel.selection.getDimension();
			
			
			RawImage afterLift = HybridHelper.createImage(afterDim.x, afterDim.y );
			gc = afterLift.getGraphics();
			gc.drawImage(img, -sel.offsetX, -sel.offsetY );
//			gc.dispose();
			
			actions.add( new SetLiftedAction(afterLift));
		}
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Transform Lifted Selection"));
	}
	

	
	
	public void imageToSelection( RawImage img, int ox, int oy) {
		if( img == null) return;
		
		List<UndoableAction> actions = new ArrayList<>(2);
		actions.add(createNewSelectAction(buildRectSelection(
				new Rect( ox, oy, img.getWidth(), img.getHeight()))));
		actions.add( new SetLiftedAction(img));
		
		undoEngine.performAndStore(undoEngine.new CompositeAction(actions, "Pasted Image to Layer"));
	}
	
	public BuiltSelection buildRectSelection( Rect rect) {
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
			GraphicsContext gc = builtImage.checkout();
			
			gc.setComposite( Composite.DST_OUT, 1.0f);
			selection.drawSelectionMask( gc);

			builtImage.checkin();
		}
	}
	
	
	public class PasteSelectionAction extends ImageAction {
		final RawImage liftedImage;
		private final BuiltSelection builtSelection;
		
		protected PasteSelectionAction(
				RawImage liftedImage2, 
				BuiltImageData builtActiveData, 
				BuiltSelection builtSelection) 
		{
			super(builtActiveData);
			this.liftedImage = liftedImage2;
			this.builtSelection = builtSelection;
		}
		@Override
		protected void performImageAction() {
			GraphicsContext gc = builtImage.checkout();
			gc.drawImage(liftedImage, builtSelection.offsetX, builtSelection.offsetY);
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
		private final RawImage newLifted, oldLifted;
		SetLiftedAction( RawImage newLifted) 
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
		private final RawImage bi;
		static int c = 0;
		
		/** NOTE: To properly draw the border, the supplied buffered image should
		 * be cropped with a 1-pixel border (if it's cropped at all).
		 */
		Selection(RawImage img) {
			this.bi = img;
		}
		
		public void drawSelectionBounds(GraphicsContext gc) {
			gc.drawBounds(bi, c);
		}
		public void drawSelectionMask(GraphicsContext gc) {
			gc.drawImage(bi, 0, 0);
		}
		public boolean contains(int x, int y) {
			if( x < 0 || x >= bi.getWidth() || y < 0 || y >= bi.getHeight())
				return false;
			
			return ((bi.getRGB(x, y) >>> 24)!= 0);
		}
		public Vec2i getDimension() {
			return new Vec2i( bi.getWidth(), bi.getHeight());
		}
		public Selection clone() {
			return new Selection(bi.deepCopy());
		}
	}

	/** BuiltSelection represents a complete Selection, including offsets and 
	 * other transforms.*/
	public static class BuiltSelection {
		public final Selection selection;
		public final int offsetX;
		public final int offsetY;

		public BuiltSelection( RawImage img) {
			Rect bounds = null;
			try {
				bounds = HybridUtil.findContentBounds(img, 2, true);
			} catch ( UnsupportedImageTypeException e) {
				MDebug.handleError(ErrorType.STRUCTURAL, e, e.getMessage());
			}
			if( bounds == null || bounds.isEmpty()) {
				selection = null;
				offsetX = 0;
				offsetY = 0;
			}
			else {
				RawImage bi2 = img;
				img = HybridHelper.createImage(bounds.width, bounds.height);
				GraphicsContext gc = img.getGraphics();
				gc.drawImage(bi2, -bounds.x, -bounds.y);
//				g.dispose();
				
				selection = new Selection(img);
				offsetX = bounds.x;
				offsetY = bounds.y;
			}
		}
		
		public BuiltSelection(Selection selection, int offsetX, int offsetY){
			this.selection = selection;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		public void drawSelectionMask(GraphicsContext gc) {
			if( selection == null) return;
			
			
			MatTrans trans = gc.getTransform();
			gc.translate(offsetX, offsetY);
			
			selection.drawSelectionMask(gc);
			
			gc.setTransform(trans);
		}
		
		private RawImage liftSelection( LiftScheme liftScheme) {
			Rect selectionRect = new Rect(selection.getDimension());
			selectionRect.x = this.offsetX;
			selectionRect.y = this.offsetY;
			
			RawImage img = HybridHelper.createImage(selectionRect.width, selectionRect.height);
			GraphicsContext gc = img.getGraphics();

			// Draw the mask, clipping the bounds of drawing to only the part 
			// that the selection	intersects with the Image so that you do not 
			//  leave un-applied mask left in the image.
			Rect dataRect = liftScheme.getBounds();
			Rect intersection = dataRect.intersection(selectionRect);
			
			// TODO
			((AWTContext)gc).getGraphics().setClip(
					intersection.x - selectionRect.x, intersection.y - selectionRect.y, 
					intersection.width, intersection.height);
			selection.drawSelectionMask(gc);	// Note: Untransformed
			

			// Copy the data inside the Selection's alphaMask to liftedData
			gc.setComposite( Composite.SRC_IN, 1.0f);

			gc.translate(-this.offsetX, -this.offsetY);
			
			liftScheme.draw(gc);
//			g2.dispose();
			
			return img;
		}
		
		/** Uses the BuiltSelection to lift the selected portion of the given
		 * BufferedImage and put it in a new BufferedImage.*/
		public RawImage liftSelectionFromImage( 
				RawImage nodeImg, int offsetX, int offsetY)
		{
			return liftSelection( new LiftScheme() {
				@Override
				public Rect getBounds() {
					return new Rect( offsetX, offsetY, nodeImg.getWidth(), nodeImg.getHeight());
				}
				
				@Override
				public void draw(GraphicsContext gc) {
					gc.drawImage(nodeImg, 0, 0);
				}
			});
		}

		/** Uses the BuiltSelection to lift the selected portion of the given
		 * BuiltImageData and put it in a new BufferedImage.*/
		public RawImage liftSelectionFromData( BuiltImageData data) {
			return liftSelection(new LiftScheme() {
				@Override
				public Rect getBounds() {
					return data.getBounds();
				}
				
				@Override
				public void draw(GraphicsContext gc) {
					data.draw(gc);
				}
			});
		}
	}
	/** Helper Class to reduce duplicate code. */
	private interface LiftScheme {
		void draw(GraphicsContext gc);
		Rect getBounds();
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
			RawImage img = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
			GraphicsContext gc = img.getGraphics();
			gc.fillRect(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
//			g.dispose();
			
			return new BuiltSelection( img);
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
			RawImage img = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());
			GraphicsContext gc = img.getGraphics();
			gc.fillOval(
					Math.min(startX, currentX), Math.min(startY, currentY),
					Math.abs(startX-currentX), Math.abs(startY-currentY));
//			g.dispose();
			
			return new BuiltSelection( img);
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
		IntCompactor compactor_x = new IntCompactor();
		IntCompactor compactor_y = new IntCompactor();
		@Override
		protected void start(int x, int y) {
			compactor_x.add(x);
			compactor_y.add(y);
		}

		@Override
		protected void update(int x, int y) {
			compactor_x.add(x);
			compactor_y.add(y);
		}
		@Override
		protected BuiltSelection build() {
			RawImage img = HybridHelper.createImage( workspace.getWidth(), workspace.getHeight());

			GraphicsContext gc = img.getGraphics();
			gc.setColor(Colors.WHITE);
			gc.fillPolygon( compactor_x.toArray(), compactor_y.toArray(), compactor_x.size());
			
			return new BuiltSelection(img);
		}

		@Override
		protected void draw(GraphicsContext g) {
			for( int i=0; i < compactor_x.getChunkCount(); ++i) {
				g.drawPolyLine(compactor_x.getChunk(i), 
							compactor_y.getChunk(i), 
							compactor_x.getChunkSize(i));
			}
		}
		public Vec2i getStart() {
			return new Vec2i( compactor_x.get(0), compactor_y.get(0));
		}
		public Vec2i getEnd() {
			int s = compactor_x.size();
			return new Vec2i( compactor_x.get(s-1), compactor_y.get(s-1));
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
