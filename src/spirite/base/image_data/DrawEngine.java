package spirite.base.image_data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.brains.MasterControl;
import spirite.base.brains.SettingsManager;
import spirite.base.brains.ToolsetManager.ColorChangeScopes;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.images.IBuiltImageData;
import spirite.base.image_data.layers.Layer;
import spirite.base.pen.PenTraits;
import spirite.base.pen.PenTraits.PenDynamics;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.STATE;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.DirectDrawer;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;
//import spirite.pc.graphics.ImageBI;

/***
 * Pretty much anything which alters the image data directly goes 
 * through the DrawEngine.
 * 
 * @author Rory Burks
 *
 */
public class DrawEngine {
	private final ImageWorkspace workspace;
	private final UndoEngine undoEngine;
	private final SelectionEngine selectionEngine;
	private final SettingsManager settingsManager;
	private StrokeEngine activeEngine = null;
	
	public DrawEngine( ImageWorkspace workspace, MasterControl master) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();
		this.selectionEngine = workspace.getSelectionEngine();
		this.settingsManager = master.getSettingsManager();
		
	}
	
	public boolean strokeIsDrawing() {return activeEngine != null;}
	public StrokeEngine getStrokeEngine() { return activeEngine; }
	public ImageHandle getStrokeContext() {
		return (activeEngine == null) ? null : activeEngine.getImageData().handle;
	}
	
	private BuildingImageData workingData;
	/** Starts a Stroke with the provided parameters
	 * 
	 * @return true if the stroke started, false otherwise	 */
	public boolean startStroke(StrokeEngine.StrokeParams stroke, PenState ps, BuildingImageData data) {
		workingData = data;
		if( activeEngine != null) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to draw two strokes at once within the DrawEngine (if you need to do that, manually instantiate a separate StrokeEngine.");
			return false;
		}
		else if( data == null) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Tried to start stroke on null data.");
			return false;
		}
		else {
//			if( activeEngine instanceof AWTStrokeEngine) stroke.setInterpolationMethod(InterpolationMethod.NONE);
			activeEngine = settingsManager.getDefaultDrawer().getStrokeEngine();
			
			if( activeEngine.startStroke(stroke, ps, workspace.buildData(data), pollSelectionMask()))
				data.handle.refresh();
			return true;
		}
	}
	
	/** Updates the active stroke. */
	public void stepStroke( PenState ps) {
		if( activeEngine == null || activeEngine.getState() != STATE.DRAWING) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to step stroke that isn't active.");
			return ;
		}
		else {
			if(activeEngine.stepStroke(ps))
				activeEngine.getImageData().handle.refresh();
		}
	}
	/** Ends the active stroke. */
	public void endStroke( ) {
		if( activeEngine == null || activeEngine.getState() != STATE.DRAWING) {
			activeEngine = null;
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to end stroke that isn't active.");
			return ;
		}
		else {
			activeEngine.endStroke();
				
			undoEngine.storeAction(
				new StrokeAction(
					activeEngine,
					activeEngine.getParams(),
					activeEngine.getHistory(),
					activeEngine.getLastSelection(),
					workingData));
			activeEngine = null;
		}
		
	}
	

	/** Clears the Image Dat to a transparent color. */
	public void clear( BuildingImageData data) {
		execute( new ClearAction(data, pollSelectionMask()));
	}

	/***
	 * Simple queue-based flood fill.
	 * @return true if any changes were made
	 */
	public boolean fill( int x, int y, int color, BuildingImageData _data)
	{
		if( _data == null) return false;
		IBuiltImageData data = workspace.buildData(_data);
		
		Vec2i p = data.convert( new Vec2i(x,y));
		
		RawImage bi = data.checkoutRaw();
		if( !MUtil.coordInImage( p.x, p.y, bi)) {
			return false;
		}
		
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		if( mask.selection != null && !mask.selection.contains(x - mask.offsetX, y-mask.offsetY)) {
			return false;
		}
		if( bi.getRGB( p.x, p.y) == color) {
			return false;
		}
		data.checkin();

		execute( new FillAction(new Vec2i(x,y), color, mask, _data));

		return true;
	}
	
	/** Flips the data around either the horizontal or vertical center of the Iamge. */
	public void flip( BuildingImageData data, boolean horizontal) {
		BuiltSelection sel = selectionEngine.getBuiltSelection();
		
		if( selectionEngine.isLifted()) {
			MatTrans trans = new MatTrans();
			if( horizontal)
				trans.scale(-1, 1);
			else
				trans.scale(1, -1);
			selectionEngine.transformSelection(trans);
		}
		else if( sel == null || sel.selection == null)
			execute( new FlipAction(data, selectionEngine.getBuiltSelection(), horizontal));
		else {
			UndoableAction actions[] = new UndoableAction[2];
			actions[0] = new FlipAction(data, selectionEngine.getBuiltSelection(), horizontal);
			
			// This is kind of bad
			RawImage img = HybridHelper.createImage(workspace.getWidth(), workspace.getHeight());
			GraphicsContext gc = img.getGraphics();
			
			gc.setColor( Colors.WHITE);
			gc.fillRect(0, 0, workspace.getWidth(), workspace.getHeight());
//			g2.dispose();
			
			img = sel.liftSelectionFromImage(img, 0, 0);
			
			img = flipImage(img, horizontal);
			
			// TODO: MARK
			BuiltSelection sel2 =  new BuiltSelection( img);
			sel2 = new BuiltSelection( sel2.selection, sel2.offsetX+sel.offsetX, sel2.offsetX+sel.offsetY);
			actions[1] = selectionEngine.createNewSelectAction(sel2);
			
			undoEngine.performAndStore( undoEngine.new CompositeAction(Arrays.asList(actions), actions[0].description));
		}
	}
	
	/**
	 * Changes all pixels of one color into another color.
	 * 
	 * @param from Color which will be changed
	 * @param to Color that it'll be changed to
	 * @param scope
	 * <li>0 : Local (applies to only the active data)
	 * <li>1 : Node (applies to all ImageData in the selected Group/Layer)
	 * <li>2 : Global (applies to ALL ImageData in the workspace)
	 * @param mode 
	 * <li>0 : Converts only exact RGBA matches.
	 * <li>1 : Converts RGB matches, ignoring alpha
	 * <li>2 : Converts all RGB colors to the <code>to</code> color, preserving alpha
	 */
	public void changeColor( int from, int to, ColorChangeScopes scope, int mode) {
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		
		Node selected = null;
		
		switch( scope) {
		case LOCAL:
			BuildingImageData bid = workspace.buildActiveData();
			if( bid != null) {
				execute( new ColorChangeAction(bid, mask, from, to, mode));
			}
			break;
		case GROUP:
			// Switch statement is kind of awkward.  Just roll with it.
			selected = workspace.getSelectedNode();
			if( selected == null) return;
		case PROJECT:
			if( selected == null) 
				selected = workspace.getRootNode();

			List<UndoableAction> actions = new ArrayList<>();
			
			for( LayerNode lnode : selected.getAllLayerNodes()) {
				Layer layer = lnode.getLayer();
				
				for( BuildingImageData data : layer.getDataToBuild()) {
					data.ox += lnode.x;
					data.oy += lnode.y;
					actions.add( new ColorChangeAction(
							data,
							mask, from, to, mode));
				}
			}
			
			UndoableAction action = undoEngine.new CompositeAction(actions, "Color Change Action");
			undoEngine.performAndStore(action);
			break;
		}
	}
	public void invert(BuildingImageData data) {
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		execute( new InvertAction(data, mask));
	}
	

	// ==============
	// ==== Stroke Dynamics
	// TODO: This should probably be in some settings area
	
	public static PenDynamics getBasicDynamics() {
		return basicDynamics;
	}
	private static final PenDynamics basicDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};

	private static final PenDynamics personalDynamics = new PenTraits.LegrangeDynamics(
		Arrays.asList( new Vec2[] {
				new Vec2(0,0),
				new Vec2(0.25f,0),
				new Vec2(1,1)
			}
		)
	);
	
	public static PenDynamics getDefaultDynamics() {
		return personalDynamics;
	}
	private static final PenDynamics defaultDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};

	private void execute( MaskedImageAction action) {
		undoEngine.performAndStore(action);
	}

	// ===============
	// ==== Queued Selection
	// Because many drawing actions can filter based on Selection
	// Mask, when re-doing them the mask which was active at the time
	// has to be remembered.  This function will apply the selection mask
	// to the next draw action performed.  If there is no seletion mask
	// queued, it will use the active selection.
	
	private void queueSelectionMask( BuiltSelection mask) {
		queuedSelection = mask;
	}
	private BuiltSelection pollSelectionMask() {
		if( queuedSelection == null)
			return workspace.getSelectionEngine().getBuiltSelection();

		BuiltSelection ret = queuedSelection;
		queuedSelection = null;
		return ret;
	}
	private BuiltSelection queuedSelection = null;
	
	// ==================
	// ==== UndoableActions
	//	Note: All actions classes are public so things that peak at the UndoEngine
	//	can know exactly what actions were performed, but all Constructors are
	//	(effectively) private so that they can only be created by the DrawEngine
	
	public abstract class MaskedImageAction extends ImageAction {
		protected final BuiltSelection mask;

		private MaskedImageAction(BuildingImageData data, BuiltSelection mask) {
			super(data);
			this.mask = mask;
		}
	}
	
	public class StrokeAction extends MaskedImageAction {
		private final PenState[] points;
		private final StrokeEngine.StrokeParams params;
		private final StrokeEngine engine;
		
		// TODO: This is public for Unit Testing purposes, but that's a bad solution
		public StrokeAction( 
				StrokeEngine engine,
				StrokeEngine.StrokeParams params, 
				PenState[] points, 
				BuiltSelection mask, 
				BuildingImageData data)
		{
			super(data, mask);
			this.engine = engine;
			this.params = params;
			this.points = points;
			
			switch( params.getMethod()) {
			case BASIC:
				description = "Basic Stroke Action";
				break;
			case ERASE:
				description = "Erase Stroke Action";
				break;
			case PIXEL:
				description = "Pixel Stroke Action";
				break;
			}
		}
		
		public StrokeEngine.StrokeParams getParams() {
			return params;
		}
		
		@Override
		public void performImageAction( ) {
			queueSelectionMask(mask);
			
			IBuiltImageData built = workspace.buildData(builtImage);
			engine.batchDraw(params, points, built, mask);
		}
	}
	public class FillAction extends MaskedImageAction {
		private final Vec2i p;
		private final int color;
		
		private FillAction( Vec2i p, int c, BuiltSelection mask, BuildingImageData data) {
			super(data, mask);
			this.p = p;
			this.color = c;
			description = "Fill";
		}

		@Override
		protected void performImageAction( ) {
			RawImage img;
			Vec2i layerSpace;
			IBuiltImageData built = workspace.buildData(builtImage);
			if( mask.selection == null) {
				img = built.checkoutRaw();
				layerSpace = built.convert( new Vec2i(p.x, p.y));
			}
			else {
				img = mask.liftSelectionFromData(built);
				layerSpace = new Vec2i(p.x - mask.offsetX, p.y - mask.offsetY);
			}

			RawImage intermediate = null;

			int bg = img.getRGB(layerSpace.x, layerSpace.y);
			
			if( mask.selection != null && bg == 0){
				// A lot of work for a singular yet common case: 
				// When coloring into transparent data, create an image which has
				//	a color other than 0 (pure transparent) outside of its selection
				//	mask (this has to be done in a couple of renderings).
				intermediate = img;
				img = HybridHelper.createImage( img.getWidth(), img.getHeight());
				
				GraphicsContext gc = img.getGraphics();
				gc.setColor( Colors.GREEN);
				gc.fillRect(0, 0, img.getWidth(), img.getHeight());
				gc.setComposite( Composite.CLEAR, 1.0f);
				mask.selection.drawSelectionMask( gc);
				gc.setComposite( Composite.SRC_OVER, 1.0f);
				gc.drawImage(intermediate, 0, 0 );
//				gc.dispose();
			}

			DirectDrawer.fill(img, layerSpace.x, layerSpace.y, color);


			
			if( mask.selection != null) {
				if( bg == 0) { 
					// Continuing from above, after the fill is done, crop out the
					//	green outer mask out of the result image.  (This requires
					//	re-using the second BufferedImage since selection masks will
					//	most often be using a geometric rendering that never actually
					//	touches the pixels outside of it with its rasterizer)
					GraphicsContext gc = intermediate.getGraphics();
					gc.clear();
					mask.selection.drawSelectionMask( gc);
//					g2.dispose();
					
					gc = img.getGraphics();
//					g2 = (Graphics2D) bi.getGraphics();
					gc.setComposite( Composite.DST_IN, 1.0f);;
					gc.drawImage(intermediate, 0, 0 );
//					g2.dispose();
				}

				// Anchor the lifted image to the real image
				GraphicsContext gc = built.checkout();
				Vec2i p = built.convert(new Vec2i(mask.offsetX,mask.offsetY));
				gc.drawImage( img, p.x, p.y);
			}
			built.checkin();
		}
		public Vec2i getPoint() { return new Vec2i(p);}
		public int getColor() { return color;}
	}

	public class ClearAction extends MaskedImageAction {
		private ClearAction(BuildingImageData data, BuiltSelection mask) {
			super(data, mask); 
			description = "Clear Image";
		}
		@Override
		protected void performImageAction() {
			IBuiltImageData built = workspace.buildData(builtImage);
			if( mask.selection == null) {
				built.checkout().clear();
				built.checkin();
			}
			else {
				GraphicsContext gc = built.checkout();
				gc.translate(mask.offsetX, mask.offsetY);
				gc.setComposite(Composite.DST_OUT, 1);
				mask.selection.drawSelectionMask(gc);
				built.checkin();
			}
		}
	}
	
	public class FlipAction extends MaskedImageAction 
	{
		private final boolean horizontal;
		private FlipAction(BuildingImageData data, BuiltSelection mask, boolean horizontal) {
			super(data, mask);
			this.horizontal = horizontal;
			description = "Flip Action";
		}

		@Override
		protected void performImageAction() {
			IBuiltImageData built = workspace.buildData(builtImage);
			
			if( mask != null && mask.selection != null) {
				
				RawImage lifted = mask.liftSelectionFromData(built);

				RawImage buffer = flipImage(lifted, horizontal);

				GraphicsContext gc = built.checkout();
				gc.setComposite( Composite.DST_OUT, 1.0f);
				mask.drawSelectionMask( gc);

				gc.setComposite(Composite.SRC_OVER, 1.0f);
				gc.drawImage(buffer, mask.offsetX, mask.offsetY);
//				gc.dispose();
				
				buffer.flush();
			}
			else {
				RawImage bi = built.checkoutRaw();
				RawImage buffer = flipImage( bi, horizontal);
				
				GraphicsContext gc = bi.getGraphics();
				gc.setComposite( Composite.SRC, 1.0f);
				gc.drawImage(buffer, 0, 0);
//				g2.dispose();
				buffer.flush();
			}
			built.checkin();
			
		}
	}
	private static RawImage flipImage( RawImage img, boolean horizontal) {
		// Might be able to do this single-Image but things get weird if you 
		//	draw a Buffer onto itself
		RawImage buffer = HybridHelper.createImage( img.getWidth(), img.getHeight());
		GraphicsContext gc = buffer.getGraphics();
		
		if( horizontal) {
			gc.translate(img.getWidth(), 0);
			gc.scale(-1.0, 1.0);
		}
		else {
			gc.translate(0, img.getHeight());
			gc.scale(1.0, -1.0);
		}
		gc.drawImage(img, 0, 0);
//		g2.dispose();
		
		return buffer;
	}

	public class ScaleAction extends MaskedImageAction 
	{
		private ScaleAction(BuildingImageData data, BuiltSelection mask) {
			super(data, mask);
		}
		
		@Override
		protected void performImageAction() {
		}
	}
	
	public abstract class PerformFilterAction extends MaskedImageAction
	{

		private PerformFilterAction(BuildingImageData data, BuiltSelection mask) {
			super(data, mask);
		}

		@Override
		protected void performImageAction() {
			IBuiltImageData built = workspace.buildData(builtImage);
			if( mask != null && mask.selection != null) {
				// Lift the Selection
				RawImage lifted = mask.liftSelectionFromData(built);
				applyFilter(lifted);

				GraphicsContext gc = built.checkout();
				gc.setComposite( Composite.DST_OUT, 1.0f);
				mask.drawSelectionMask(gc);

				gc.setComposite( Composite.SRC_OVER, 1.0f);
				gc.drawImage( lifted, mask.offsetX, mask.offsetY );
			}
			else {
				RawImage bi = built.checkoutRaw();
				applyFilter(bi);
			}
			built.checkin();
		}
		
		abstract void applyFilter( RawImage image);
		
	}
	
	public class ColorChangeAction extends PerformFilterAction 
	{
		private final int from, to;
		private final int mode;
		private ColorChangeAction(
				BuildingImageData data, 
				BuiltSelection mask, 
				int from, int to, 
				int mode) 
		{
			super(data, mask);
			this.from = from;
			this.to = to;
			this.mode = mode;
			description = "Color Change Action";
		}
		@Override
		void applyFilter(RawImage image) {
			settingsManager.getDefaultDrawer().changeColor(image, from, to, mode);
		}
	}
	
	public class InvertAction extends PerformFilterAction {
		private InvertAction(BuildingImageData data, BuiltSelection mask) {
			super(data, mask);
		}
		@Override
		void applyFilter(RawImage image) {
			settingsManager.getDefaultDrawer().invert(image);
		}
	}
}
