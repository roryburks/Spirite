package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.BuildingImageData;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.image_data.UndoEngine.ImageAction;
import spirite.image_data.UndoEngine.UndoableAction;
import spirite.image_data.layers.Layer;
import spirite.pen.PenTraits;
import spirite.pen.PenTraits.PenDynamics;
import spirite.pen.PenTraits.PenState;
import spirite.pen.StrokeEngine;
import spirite.pen.StrokeEngine.STATE;
import spirite.pen.StrokeEngine.StrokeParams.InterpolationMethod;

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
	
	// TODO: I don't necessarily like DrawEngine having MasterAccess, but it needs
	//	access to the current GraphicsContext at all times
	private final MasterControl master;
	
	private StrokeEngine activeEngine = null;
	
	public DrawEngine( ImageWorkspace workspace, MasterControl master) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();
		this.selectionEngine = workspace.getSelectionEngine();
		
		this.master = master;
	}
	
	public boolean strokeIsDrawing() {
		return activeEngine != null;
	}
	public StrokeEngine getStrokeEngine() {
		return activeEngine;
	}
	public StrokeEngine ___J_defEngine() {
		return master.getGraphicsContext().getStrokeEngine();
	}
	public ImageHandle getStrokeContext() {
		return (activeEngine == null) ? null : activeEngine.getImageData().handle;
	}
	
	/** @return true if the stroke started, false otherwise	 */
	public boolean startStroke(StrokeEngine.StrokeParams stroke, PenState ps, BuiltImageData data) {
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
			activeEngine = master.getGraphicsContext().getStrokeEngine();
			
			if( activeEngine.startStroke(stroke, ps, data, pollSelectionMask()))
				data.handle.refresh();
			return true;
		}
	}
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
					activeEngine.getImageData()));
			activeEngine = null;
		}
		
	}
	

	/***
	 * 
	 */
	public void clear( BuiltImageData data) {
		execute( new ClearAction(data, pollSelectionMask()));
	}

	/***
	 * Simple queue-based flood fill.
	 * @return true if any changes were made
	 */
	public boolean fill( int x, int y, Color color, BuiltImageData data)
	{
		if( data == null) return false;
		
		Point p = data.convert( new Point(x,y));
		
		BufferedImage bi = data.checkoutRaw();
		if( !MUtil.coordInImage( p.x, p.y, bi)) {
			return false;
		}
		
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		if( mask.selection != null && !mask.selection.contains(x - mask.offsetX, y-mask.offsetY)) {
			return false;
		}
		if( bi.getRGB( p.x, p.y) == color.getRGB()) {
			return false;
		}
		data.checkin();

		execute( new FillAction(new Point(x,y), color, mask, data));

		return true;
	}
	
	public void flip( BuiltImageData data, boolean horizontal) {
		BuiltSelection sel = selectionEngine.getBuiltSelection();
		
		if( selectionEngine.isLifted()) {
			AffineTransform trans = new AffineTransform();
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
			BufferedImage bi = new 
					BufferedImage( workspace.getWidth(), workspace.getHeight(), Globals.BI_FORMAT);
			Graphics2D g2 = (Graphics2D) bi.getGraphics();
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, workspace.getWidth(), workspace.getHeight());
			g2.dispose();
			
			bi = sel.liftSelectionFromImage(bi, 0, 0);
			bi = flipImage(bi, horizontal);
			
			BuiltSelection sel2 =  new BuiltSelection(bi);
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
	public void changeColor( Color from, Color to, int scope, int mode) {
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		
		Node selected = null;
		
		switch( scope) {
		case 0:	// Local
			BuiltImageData bid = workspace.buildActiveData();
			if( bid != null) {
				execute( new ColorChangeAction(bid, mask, from, to, mode));
			}
			break;
		case 1: // Group/Layer
			// Switch statement is kind of awkward.  Just roll with it.
			selected = workspace.getSelectedNode();
			if( selected == null) return;
		case 2: // Global
			if( selected == null) 
				selected = workspace.getRootNode();

			List<UndoableAction> actions = new ArrayList<>();
			
			for( LayerNode lnode : selected.getAllLayerNodes()) {
				Layer layer = lnode.getLayer();
				
				for( BuildingImageData data : layer.getDataToBuild()) {
					data.ox += lnode.x;
					data.oy += lnode.y;
					actions.add( new ColorChangeAction(
							workspace.buildData(data),
							mask, from, to, mode));
				}
			}
			
			UndoableAction action = undoEngine.new CompositeAction(actions, "Color Change Action");
			undoEngine.performAndStore(action);
			break;
		}
	}
	public void invert(BuiltImageData data) {
		BuiltSelection mask = selectionEngine.getBuiltSelection();
		execute( new InvertAction(data, mask));
	}
	


	// Defauilt Dynamics
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
		Arrays.asList( new Point2D[] {
				new Point2D.Double(0,0),
				new Point2D.Double(0.25,0),
				new Point2D.Double(1,1)
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
		action.performImageAction();
		undoEngine.storeAction(action);
	}

	
	// :::: Other
	
	
	
	

	/** Because many drawing actions can filter based on Selection
	 * Mask, when re-doing them the mask which was active at the time
	 * has to be remembered.  This function will apply the selection mask
	 * to the next draw action performed.  If there is no seletion mask
	 * queued, it will use the active selection.
	 */
	void queueSelectionMask( BuiltSelection mask) {
		queuedSelection = mask;
	}
	private BuiltSelection pollSelectionMask() {
		if( queuedSelection == null)
			return workspace.getSelectionEngine().getBuiltSelection();

		BuiltSelection ret = queuedSelection;
		queuedSelection = null;
		return ret;
	}
	BuiltSelection queuedSelection = null;
	
	// :::: UndoableActions
	//	All actions 
	
	public abstract class MaskedImageAction extends ImageAction {
		protected final BuiltSelection mask;

		MaskedImageAction(BuiltImageData data, BuiltSelection mask) {
			super(data);
			this.mask = mask;
		}
	}
	
	public class StrokeAction extends MaskedImageAction {
		private final PenState[] points;
		private final StrokeEngine.StrokeParams params;
		private final StrokeEngine engine;
		
		public StrokeAction( 
				StrokeEngine engine,
				StrokeEngine.StrokeParams params, 
				PenState[] points, 
				BuiltSelection mask, 
				BuiltImageData data)
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

			engine.batchDraw(params, points, builtImage, mask);
		}
	}
	public class FillAction extends MaskedImageAction {
		private final Point p;
		private final Color color;
		
		public FillAction( Point p, Color c, BuiltSelection mask, BuiltImageData data) {
			super(data, mask);
			this.p = p;
			this.color = c;
			description = "Fill";
		}

		@Override
		protected void performImageAction( ) {
			BufferedImage bi;
			Point layerSpace;
			BufferedImage intermediate = null;
			if( mask.selection == null) {
				bi= builtImage.checkoutRaw();
				layerSpace = builtImage.convert( new Point(p.x, p.y));
			}
			else {
				bi = mask.liftSelectionFromData(builtImage);
				layerSpace = new Point(p.x - mask.offsetX, p.y - mask.offsetY);
			}
			
			Queue<Integer> queue = new LinkedList<Integer>();
			
			queue.add( MUtil.packInt(layerSpace.x, layerSpace.y));
			
			int w = bi.getWidth();
			int h = bi.getHeight();
			int bg = bi.getRGB(layerSpace.x, layerSpace.y);
			int c = color.getRGB();
			
			if( mask.selection != null && bg == 0){
				// A lot of work for a singular yet common case: 
				// When coloring into transparent data, create an image which has
				//	a color other than 0 (pure transparent) outside of its selection
				//	mask (this has to be done in a couple of renderings).
				intermediate = bi;
				bi = new BufferedImage(
						bi.getWidth(), bi.getHeight(), Globals.BI_FORMAT);
				Graphics2D g2 = (Graphics2D) bi.getGraphics();
				g2.setColor(Color.GREEN);
				g2.fillRect(0, 0, bi.getWidth(), bi.getHeight());
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
				mask.selection.drawSelectionMask(g2);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
				g2.drawImage(intermediate, 0, 0, null);
				g2.dispose();
			}
			
			
			if( bg == c) return;
			
			while( !queue.isEmpty()) {
				int p = queue.poll();
				int ix = MUtil.high16(p);
				int iy = MUtil.low16(p);
				
				if( bi.getRGB(ix, iy) != bg)
					continue;
					
					
				bi.setRGB(ix, iy, c);

				if( ix + 1 < w) {
					queue.add( MUtil.packInt(ix+1, iy));
				}
				if( ix - 1 >= 0) {
					queue.add( MUtil.packInt(ix-1, iy));
				}
				if( iy + 1 < h) {
					queue.add( MUtil.packInt(ix, iy+1));
				}
				if( iy - 1 >= 0) {
					queue.add( MUtil.packInt(ix, iy-1));
				}
			}
			
			if( mask.selection != null) {
				if( bg == 0) { 
					// Continuing from above, after the fill is done, crop out the
					//	green outer mask out of the result image.  (This requires
					//	re-using the second BufferedImage since selection masks will
					//	most often be using a geometric rendering that never actually
					//	touches the pixels outside of it with its rasterizer)
					MUtil.clearImage(intermediate);
					Graphics2D g2 = (Graphics2D)intermediate.getGraphics();
					mask.selection.drawSelectionMask(g2);
					g2.dispose();
					
					g2 = (Graphics2D) bi.getGraphics();
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
					g2.drawImage(intermediate, 0, 0, null);
					g2.dispose();
				}

				// Anchor the lifted image to the real image
				Graphics g = builtImage.checkout();
				Point p = builtImage.convert(new Point(mask.offsetX,mask.offsetY));
				g.drawImage( bi, p.x, p.y, null);
			}
			builtImage.checkin();
		}
		public Point getPoint() { return new Point(p);}
		public Color getColor() { return new Color(color.getRGB());}
	}

	public class ClearAction extends MaskedImageAction {
		private ClearAction(BuiltImageData data, BuiltSelection mask) {
			super(data, mask); 
			description = "Clear Image";
		}
		@Override
		protected void performImageAction() {
			
			if( mask.selection == null) {
				builtImage.checkout();
				MUtil.clearImage(builtImage.handle.deepAccess());
				builtImage.checkin();
			}
			else {
				Graphics2D g2 = (Graphics2D) builtImage.checkout();
				g2.translate(mask.offsetX, mask.offsetY);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
				mask.selection.drawSelectionMask(g2);
				builtImage.checkin();
			}

		}
	}
	
	public class FlipAction extends MaskedImageAction 
	{
		private final boolean horizontal;
		FlipAction(BuiltImageData data, BuiltSelection mask, boolean horizontal) {
			super(data, mask);
			this.horizontal = horizontal;
			description = "Flip Action";
		}

		@Override
		protected void performImageAction() {
			
			if( mask != null && mask.selection != null) {
				
				BufferedImage lifted = mask.liftSelectionFromData(builtImage);

				BufferedImage buffer = flipImage(lifted, horizontal);

				Graphics2D g2 = (Graphics2D) builtImage.checkout();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
				mask.drawSelectionMask(g2);
				

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
				g2.drawImage(buffer, mask.offsetX, mask.offsetY, null);
				g2.dispose();
				buffer.flush();
			}
			else {
				BufferedImage bi = builtImage.checkoutRaw();
				BufferedImage buffer = flipImage( bi, horizontal);
				
				Graphics2D g2 = (Graphics2D) bi.getGraphics();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
				g2.drawImage(buffer, 0, 0, null);
				g2.dispose();
				buffer.flush();
			}
			builtImage.checkin();
			
		}
	}
	private static BufferedImage flipImage( BufferedImage bi, boolean horizontal) {
		// Might be able to do this single-Image but things get weird if you 
		//	draw a Buffer onto itself
		BufferedImage buffer = new BufferedImage( 
				bi.getWidth(), bi.getHeight(), Globals.BI_FORMAT);
		Graphics2D g2 = (Graphics2D)buffer.getGraphics();
		
		if( horizontal) {
			g2.translate(bi.getWidth(), 0);
			g2.scale(-1.0, 1.0);
		}
		else {
			g2.translate(0, bi.getHeight());
			g2.scale(1.0, -1.0);
		}
		g2.drawImage(bi, 0, 0, null);
		g2.dispose();
		
		return buffer;
	}

	public class ScaleAction extends MaskedImageAction 
	{
		ScaleAction(BuiltImageData data, BuiltSelection mask) {
			super(data, mask);
		}
		
		@Override
		protected void performImageAction() {
		}
	}
	
	public abstract class PerformFilterAction extends MaskedImageAction
	{

		PerformFilterAction(BuiltImageData data, BuiltSelection mask) {
			super(data, mask);
		}

		@Override
		protected void performImageAction() {
			if( mask != null && mask.selection != null) {
				// Lift the Selection
				BufferedImage lifted = mask.liftSelectionFromData(builtImage);
				applyFilter(lifted);

				Graphics2D g2 = (Graphics2D) builtImage.checkout();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
				mask.drawSelectionMask(g2);

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
				g2.drawImage( lifted, mask.offsetX, mask.offsetY, null);
			}
			else {
				BufferedImage bi = builtImage.checkoutRaw();
				applyFilter(bi);
			}
			builtImage.checkin();
		}
		
		abstract void applyFilter( BufferedImage image);
		
	}
	public class ColorChangeAction extends PerformFilterAction 
	{
		private final Color from, to;
		private final int mode;
		ColorChangeAction(
				BuiltImageData data, 
				BuiltSelection mask, 
				Color from, Color to, 
				int mode) 
		{
			super(data, mask);
			this.from = from;
			this.to = to;
			this.mode = mode;
			description = "Color Change Action";
		}
		@Override
		void applyFilter(BufferedImage image) {
			workspace.getGraphicsContext().changeColor(image, from, to, mode);
		}
	}
	public class InvertAction extends PerformFilterAction {
		InvertAction(BuiltImageData data, BuiltSelection mask) {
			super(data, mask);
		}
		@Override
		void applyFilter(BufferedImage image) {
			workspace.getGraphicsContext().invert(image);
		}
	}
	

	public BufferedImage scale(BufferedImage bi) {

		// Might be able to do this single-Image but things get weird if you 
		//	draw a Buffer onto itself
		BufferedImage buffer = new BufferedImage( 
				bi.getWidth()*2, bi.getHeight()*2, Globals.BI_FORMAT);
		Graphics2D g2 = (Graphics2D)buffer.getGraphics();
		
		g2.scale(2, 2);
		
		g2.drawImage(bi, 0, 0, null);
		g2.dispose();
		
		return buffer;
	}
	
}
