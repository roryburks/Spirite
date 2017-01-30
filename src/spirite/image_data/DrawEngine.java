package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.image_data.UndoEngine.ImageAction;

/***
 * Pretty much anything which alters the image data directly goes 
 * through the DrawEngine.
 * 
 * 
 * @author Rory Burks
 *
 */
public class DrawEngine {
	private final ImageWorkspace workspace;
	private final StrokeEngine engine = new StrokeEngine();
	private final UndoEngine undoEngine;
	private final SelectionEngine selectionEngine;
	
	public DrawEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();
		this.selectionEngine = workspace.getSelectionEngine();
	}

	public StrokeEngine startStrokeEngine( BuiltImageData data) {
		engine.data = data;
		engine.stroke=  null;
		return engine;
	}
	
	public boolean strokeIsDrawing() {
		return (engine.state == STATE.DRAWING);
	}
	public BufferedImage getStrokeLayer() {
		return engine.strokeLayer;
	}
	public StrokeEngine getStrokeEngine() {
		return engine;
	}
	public ImageHandle getStrokeContext() {
		if( engine.state == STATE.DRAWING) {
			return engine.data.handle;
		}
		else
			return null;
	}
	
	
	
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
	
	
	
	// Stroke Code
	private enum STATE { READY, DRAWING };
	
	public static class PenState {
		PenState(){}
		public PenState( int x, int y, float pressure) {
			this.x = x;
			this.y = y;
			this.pressure = pressure;
		}
		public PenState( PenState other) {
			this.x = other.x;
			this.y = other.y;
			this.pressure = other.pressure;
		}
		public int x;
		public int y;
		public float pressure = 1.0f;
	}
	
	public static interface PenDynamics {
		public float getSize( PenState ps);
	}
	

	public static PenDynamics getBasicDynamics() {
		return basicDynamics;
	}
	private static final PenDynamics basicDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return 1.0f;
		}
	};
	
	public static PenDynamics getDefaultDynamics() {
		return defaultDynamics;
	}
	private static final PenDynamics defaultDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return ps.pressure;
		}
	};
	
	/***
	 * The StrokeEngine is abstracted from the DrawEngine primarily for style
	 * purposes, but you could presumably create multiple stroke engines to
	 * draw on data asynchronously, though the stroke wouldn't appear until it
	 * was completed.
	 * 
	 * The StrokeEngine operates asynchronously to the input data.  In general
	 * the stroke is only drawn at a rate of 60FPS regardless of how fast the 
	 * pen input is performed.
	 * 
	 * The StrokeEngine creates three BufferedImages the size of the ImageData
	 * in question:
	 * -The strokeLayer stores the actual stroke visually.  Strokes are drawn on 
	 *   this layer before being anchored to the ImageData layer at the end of the
	 *   stroke so that transparency and other blend methods can be performed without
	 *   worrying about the stroke drawing over itself.
	 * -The compositionLayer is stored for the benefit of ImageData which needs
	 *   another layer in order for certain blend modes/Stroke styles to properly
	 *   render
	 * -The selectionMask is cached because the memory waste is minimal compared
	 *   to the amount of extra cycles it'd be to constantly draw an inverse mask
	 *   of the selection.
	 */
	public class StrokeEngine {
		private PenState oldState = new PenState();
		private PenState newState = new PenState();
		private PenState rawState = new PenState();	// Needed to prevent UndoAction from double-tranforming
		private STATE state = STATE.READY;

		private StrokeParams stroke;
		private BuiltImageData data;
		private BufferedImage strokeLayer;
		private BufferedImage compositionLayer;
		private BufferedImage selectionMask;

		private BuiltSelection sel;

		// Records the rawState
		private List<PenState> prec = new LinkedList<>();
		
		protected StrokeEngine() {
			stroke = null;
		}
		
		// :::: Get's
		public StrokeParams getParams() {
			return stroke;
		}
		public BuiltImageData getImageData() {
			return data;
		}
		
		/**
		 * Starts a new stroke using the workspace's current selection as the 
		 * selection mask 
		 * 
		 * @return true if the data has been changed, false otherwise*/
		public synchronized boolean startStroke( StrokeParams s, PenState ps) {

			if( data == null) 
				return false;
			stroke = s;
			
			strokeLayer = new BufferedImage( 
					data.handle.getWidth(), data.handle.getHeight(), BufferedImage.TYPE_INT_ARGB);
			compositionLayer = new BufferedImage( 
					data.handle.getWidth(), data.handle.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int crgb = stroke.getColor().getRGB();
			
			sel = pollSelectionMask();
			
			if( sel.selection != null) {
				selectionMask = new BufferedImage( 
						data.handle.getWidth(), data.handle.getHeight(), BufferedImage.TYPE_INT_ARGB);
				MUtil.clearImage(selectionMask);
				
				Graphics2D g2 = (Graphics2D)selectionMask.getGraphics();
				g2.translate(sel.offsetX, sel.offsetY);
				sel.selection.drawSelectionMask(g2);
				g2.dispose();
			}
			
			// Starts recording the Pen States
			prec = new LinkedList<PenState>();
			Point layerSpace = (data.convert(new Point(ps.x,ps.y)));
			
			oldState.x = layerSpace.x;
			oldState.y = layerSpace.y;
			oldState.pressure = ps.pressure;
			newState.x = layerSpace.x;
			newState.y = layerSpace.y;
			newState.pressure = ps.pressure;
			rawState.x = ps.x;
			rawState.y = ps.y;
			rawState.pressure = ps.pressure;
			prec.add( ps);
			
			state = STATE.DRAWING;
			
			
			if( MUtil.coordInImage( layerSpace.x, layerSpace.y, strokeLayer) 
					&& strokeLayer.getRGB( layerSpace.x, layerSpace.y) != crgb) 
			{
				strokeLayer.setRGB( layerSpace.x, layerSpace.y, crgb);
				return true;
			}
			return false;
		}
		
		/***
		 * Draws the next step in the stroke, assuming that updateStroke was
		 * already called to update the PenState
		 * 
		 * @return true if the step wan't a null-step (non-moving)
		 */
		public synchronized boolean stepStroke() {
			if( state != STATE.DRAWING || data == null) {
				MDebug.handleWarning( WarningType.STRUCTURAL, this, "Data Dropped mid-stroke (possible loss of Undo functionality)");
				endStroke();
				return false;
			}
			
			boolean changed = false;
				
			// Draw Stroke (only if the mouse has moved)
			if( newState.x != oldState.x || newState.y != oldState.y)
			{
				prec.add( new PenState(rawState));
				Graphics g = strokeLayer.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				g.setColor( stroke.getColor());

				if( stroke.getMethod() != Method.PIXEL){
					g2.setStroke( new BasicStroke( 
							stroke.dynamics.getSize(newState)*stroke.width, 
							BasicStroke.CAP_ROUND, 
							BasicStroke.CAP_SQUARE));
				}
				g2.drawLine( oldState.x, oldState.y, newState.x, newState.y);
				

				if( sel.selection != null) {
					g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
					g2.drawImage(selectionMask, 0, 0, null);
				}
				
				g.dispose();
				changed = true;
			}
			
			
			oldState.x = newState.x;
			oldState.y = newState.y;
			oldState.pressure = newState.pressure;
			return changed;
		}
		
		/** Updates the coordinates for the stroke.
		 * 
		 * DOES NOT ATUALLY DRAW THE STROKE (call stepStroke for that). */
		public synchronized void updateStroke( PenState state) {
			Point layerSpace = data.convert( new Point( state.x, state.y));
			newState.x = layerSpace.x;
			newState.y = layerSpace.y;
			newState.pressure = state.pressure;
			rawState.x = state.x;
			rawState.y = state.y;
			rawState.pressure = state.pressure;
		}
		
		/** Finalizes the stroke, resetting the state, anchoring the strokeLayer
		 * to the data, and flushing the used resources. */
		public synchronized void endStroke() {
			state = STATE.READY;
			
			if( data != null) {
				BufferedImage bi = workspace.checkoutImage(data.handle);
				Graphics g = bi.getGraphics();
				drawStrokeLayer(g);
				g.dispose();
				workspace.checkinImage(data.handle);
			}
			
			strokeLayer.flush();
			compositionLayer.flush();
			if( selectionMask != null)
				selectionMask.flush();
		}
		
		// Methods used to record the Stroke so that it can be repeated
		//	Could possibly combine them into a single class
		public PenState[] getHistory() {
			PenState[] array = new PenState[prec.size()];
			return prec.toArray(array);
		}
		public BuiltSelection getLastSelection() {
			return sel;
		}
		

		// Draws the Stroke Layer onto the graphics
		public void drawStrokeLayer( Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			Composite c = g2.getComposite();
			switch( stroke.method) {
			case BASIC:
			case PIXEL:
				g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER,stroke.alpha));
				break;
			case ERASE:
				g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_OUT,stroke.alpha));
				break;
			}
			g.drawImage(getStrokeLayer(), 0, 0, null);
			g2.setComposite( c);
		}
		
		public BufferedImage getCompositionLayer() {
			MUtil.clearImage(compositionLayer);
			return compositionLayer;
		}
	}

	public enum Method {BASIC, ERASE, PIXEL};
	/** 
	 * StrokeParams define the style/tool/options of the Stroke.
	 * 
	 * lock is not actually used yet, but changing data mid-stroke is a 
	 * bar idea.
	 */
	public static class StrokeParams {
		
		Color c = Color.BLACK;
		Method method = Method.BASIC;
		float width = 1.0f;
		float alpha = 1.0f;
		boolean locked = false;
		PenDynamics dynamics = DrawEngine.getBasicDynamics();
		
		public StrokeParams() {}
		
		public void setColor( Color c) {
			if( !locked)
				this.c = c;
		}
		public Color getColor() {return new Color( c.getRGB());}
		
		public void setMethod( Method method) {
			if( !locked)
				this.method = method;
		}
		public Method getMethod() {return method;}
		
		public void setWidth( float width) {
			if( !locked)
				this.width = width;
		}
		public float getWidth() { return width;}
		
		public void setAlpha( float alpha) {
			if( !locked)
				this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
		}
		public float getAlpha() {return alpha;}
	}

	
	private void execute( MaskedImageAction action) {
		action.performImageAction();
		undoEngine.storeAction(action);
	}

	
	// :::: Other
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
		
		BufferedImage bi = data.handle.deepAccess();
		if( !MUtil.coordInImage( p.x, p.y, bi)) {
			System.out.println("OUT:" + p);
			return false;
		}
		if( bi.getRGB( p.x, p.y) == color.getRGB()) {
			System.out.println("DUPE");
			return false;
		}
		
		execute( new FillAction(new Point(x,y), color, selectionEngine.getBuiltSelection(), data));
		return true;
	}
	
	
	
	
	
	// :::: UndoableActions
	//	All actions 
	
	public abstract class MaskedImageAction extends ImageAction {
		protected final BuiltSelection mask;
		protected final BuiltImageData data;

		MaskedImageAction(BuiltImageData data, BuiltSelection mask) {
			super(data.handle);
			this.mask = mask;
			this.data = data;
		}
	}
	
	public class StrokeAction extends MaskedImageAction {
		private final PenState[] points;
		private final StrokeParams params;
		
		public StrokeAction( StrokeParams params, PenState[] points, BuiltSelection mask, BuiltImageData data){	
			super(data, mask);
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
		
		public StrokeParams getParams() {
			return params;
		}
		
		@Override
		public void performImageAction( ) {
			queueSelectionMask(mask);
			StrokeEngine engine = workspace.getDrawEngine().startStrokeEngine(data);
			
			engine.startStroke(params, points[0]);
			
			for( int i = 1; i < points.length; ++i) {
				engine.updateStroke( points[i]);
				engine.stepStroke();
			}
			
			engine.endStroke();
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
			
			BufferedImage bi = workspace.checkoutImage(data.handle);
			
			Queue<Integer> queue = new LinkedList<Integer>();
			
			Point layerSpace = data.convert( new Point(p.x, p.y));
			queue.add( MUtil.packInt(layerSpace.x, layerSpace.y));
			
			
			int w = bi.getWidth();
			int h = bi.getHeight();
			int bg = bi.getRGB(layerSpace.x, layerSpace.y);
			int c = color.getRGB();
			
			
			
			if( bg == c) return;
			
			while( !queue.isEmpty()) {
				int p = queue.poll();
				int ix = MUtil.high16(p);
				int iy = MUtil.low16(p);
				
				// TODO: Was bad so I removed Selection Masking for now
				//
				// Better way: lift the selected data out of the mask with a different
				//	color underneath (just have to make sure other color is different from
				//	bg), then perform fill on that layer, then paste the layer back.
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
			
			workspace.checkinImage(data.handle);
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
				data.checkout();
				MUtil.clearImage(data.handle.deepAccess());
				data.checkin();
			}
			else {
				Graphics2D g2 = (Graphics2D) data.checkout();
				g2.translate(mask.offsetX, mask.offsetY);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
				mask.selection.drawSelectionMask(g2);
				data.checkin();
			}

		}
	}
	
	
}
