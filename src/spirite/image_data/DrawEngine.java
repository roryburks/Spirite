package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;

import mutil.Interpolation;
import mutil.Interpolation.LagrangeInterpolator;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.gl.GLStrokeEngine;
import spirite.gl.JOGLDrawer;
import spirite.MUtil;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.image_data.UndoEngine.ImageAction;
import spirite.pen.PenTraits;
import spirite.pen.PenTraits.PenDynamics;
import spirite.pen.PenTraits.PenState;
import spirite.pen.StrokeEngine;
import spirite.pen.StrokeEngine.STATE;

/***
 * Pretty much anything which alters the image data directly goes 
 * through the DrawEngine.

 * 
 * @author Rory Burks
 *
 */
public class DrawEngine {
	private final ImageWorkspace workspace;
	private final DefaultStrokeEngine engine = new DefaultStrokeEngine();
//	private final StrokeEngine engine = new GLStrokeEngine();
	private final UndoEngine undoEngine;
	private final SelectionEngine selectionEngine;
	private final JOGLDrawer jogl = new JOGLDrawer();
	
	public DrawEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
		this.undoEngine = workspace.getUndoEngine();
		this.selectionEngine = workspace.getSelectionEngine();
		
		
	}
	
	public boolean strokeIsDrawing() {
		return (engine.getState() == STATE.DRAWING);
	}
	public BufferedImage getStrokeLayer() {
		return engine.getStrokeLayer();
	}
	public StrokeEngine getStrokeEngine() {
		return engine;
	}
	public ImageHandle getStrokeContext() {
		if( engine.getState() == STATE.DRAWING) {
			return engine.getImageData().handle;
		}
		else
			return null;
	}
	
	/** @return true if the stroke started, false otherwise	 */
	public boolean startStroke(StrokeParams stroke, PenState ps, BuiltImageData data) {
		if( engine.getState() == STATE.DRAWING) {
			MDebug.handleError(ErrorType.STRUCTURAL, this, "Tried to draw two strokes at once within the DrawEngine (if you need to do that, manually instantiate a separate StrokeEngine.");
			return false;
		}
		else if( data == null) {
			MDebug.handleError(ErrorType.STRUCTURAL, this, "Tried to start stroke on null data.");
			return false;
		}
		else {
			if( engine.startStroke(stroke, ps, data, pollSelectionMask()))
				data.handle.refresh();
			return true;
		}
	}
	public void stepStroke( PenState ps) {
		if( engine.getState() != STATE.DRAWING) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to step stroke that isn't active.");
			return ;
		}
		else {
			if(engine.stepStroke(ps))
				engine.getImageData().handle.refresh();
		}
	}
	public void endStroke( ) {
		if( engine.getState() != STATE.DRAWING) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Tried to end stroke that isn't active.");
			return ;
		}
		else {
				engine.endStroke();
				
				undoEngine.storeAction(
					new StrokeAction(
						engine.getParams(),
						engine.getHistory(),
						engine.getLastSelection(),
						engine.getImageData()));
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
		execute( new FlipAction(data, selectionEngine.getBuiltSelection(), horizontal));
	}
	
	public void changeColor( BuiltImageData data) {
		execute( new ColorChangeAction(data, selectionEngine.getBuiltSelection()));
	}
	
	
	
	// Stroke Code
	
	


	
	public static PenDynamics getBasicDynamics() {
		return basicDynamics;
	}
	private static final PenDynamics basicDynamics = new PenDynamics() {
		@Override
		public float getSize(PenState ps) {
			return 1.0f;
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
	
	/***
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
	public class DefaultStrokeEngine extends StrokeEngine
	{
		protected DefaultStrokeEngine() {
		}
		
		@Override
		public boolean startDrawStroke( PenState ps) {
			int crgb = stroke.getColor().getRGB();
			if( strokeLayer.getRGB( ps.x, ps.y) != crgb ) {
				strokeLayer.setRGB( ps.x, ps.y, crgb);
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
		@Override
		public boolean stepDrawStroke( PenState fromState, PenState toState) {
			// Draw Stroke (only if the mouse has moved)
			if( toState.x != fromState.x || toState.y != fromState.y)
			{
				Graphics g = strokeLayer.getGraphics();
				Graphics2D g2 = (Graphics2D)g;
				g.setColor( stroke.getColor());

				if( stroke.getMethod() != Method.PIXEL){
					g2.setStroke( new BasicStroke( 
							stroke.dynamics.getSize(toState)*stroke.width, 
							BasicStroke.CAP_ROUND, 
							BasicStroke.CAP_SQUARE));
				}
				g2.drawLine( fromState.x, fromState.y, toState.x, toState.y);
				

				if( sel.selection != null) {
					g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
					g2.drawImage(selectionMask, 0, 0, null);
				}
				
				g.dispose();
				return true;
			}
			
			
			return false;
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
		PenDynamics dynamics = DrawEngine.getDefaultDynamics();
		
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
		
		public PenDynamics getDynamics() {
			return dynamics;
		}
	}

	
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
			
			if( !engine.batchDraw(params, points, builtImage, mask)){
				engine.startStroke(params, points[0], builtImage, mask);
				for( int i = 1; i < points.length; ++i) {
					engine.stepStroke( points[i]);
				}
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
						bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
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
			try {
				ImageIO.write(bi, "png", new java.io.File("E:/test.png"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			BufferedImage bi = builtImage.checkoutRaw();
			
			if( mask != null && mask.selection != null) {
				
				BufferedImage lifted = mask.liftSelectionFromData(builtImage);


				BufferedImage buffer = flipImage(lifted);

				Graphics2D g2 = (Graphics2D) bi.getGraphics();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
				mask.drawSelectionMask(g2);
				

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
				g2.drawImage(buffer, mask.offsetX, mask.offsetY, null);
				g2.dispose();
				buffer.flush();
			}
			else {
				BufferedImage buffer = flipImage( bi);
				
				Graphics2D g2 = (Graphics2D) bi.getGraphics();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
				g2.drawImage(buffer, 0, 0, null);
				g2.dispose();
				buffer.flush();
			}
			
			builtImage.checkin();
		}

		private BufferedImage flipImage( BufferedImage bi) {
			// Might be able to do this single-Image but things get weird if you 
			//	draw a Buffer onto itself
			BufferedImage buffer = new BufferedImage( 
					bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
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
	public class ColorChangeAction extends MaskedImageAction 
	{

		ColorChangeAction(BuiltImageData data, BuiltSelection mask) {
			super(data, mask);
		}
		

		@Override
		protected void performImageAction() {
			Graphics g = builtImage.checkout();
			g.drawImage( jogl.renderTriangle(), 0, 0, null);
			builtImage.checkin();
		}
	}
	

	public BufferedImage scale(BufferedImage bi) {

		// Might be able to do this single-Image but things get weird if you 
		//	draw a Buffer onto itself
		BufferedImage buffer = new BufferedImage( 
				bi.getWidth()*2, bi.getHeight()*2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D)buffer.getGraphics();
		
		g2.scale(2, 2);
		
		g2.drawImage(bi, 0, 0, null);
		g2.dispose();
		
		return buffer;
	}
	
}
