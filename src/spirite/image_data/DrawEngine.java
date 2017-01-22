package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.MUtil;

/***
 * Pretty much anything which alters the image data directly goes 
 * through the DrawEngine
 * 
 * @author Rory Burks
 *
 */
public class DrawEngine {
	private final ImageWorkspace workspace;
	private final StrokeEngine engine = new StrokeEngine();
	
	public DrawEngine( ImageWorkspace workspace) {
		this.workspace = workspace;
	}

	public StrokeEngine startStrokeEngine( ImageData data) {
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
	public ImageData getStrokeContext() {
		if( engine.state == STATE.DRAWING) {
			return engine.data;
		}
		else
			return null;
	}

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
		int x;
		int y;
		float pressure = 1.0f;
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
	 */
	public class StrokeEngine {
		PenState oldState = new PenState();
		PenState newState = new PenState();
		STATE state = STATE.READY;

		StrokeParams stroke;
		ImageData data;
		BufferedImage strokeLayer;
		BufferedImage compositionLayer;
		
		List<PenState> prec = new LinkedList<>();
		
		protected StrokeEngine() {
			stroke = null;
		}
		
		// :::: Get's
		public StrokeParams getParams() {
			return stroke;
		}
		public ImageData getImageData() {
			return data;
		}
		
		/***
		 * 
		 * @param s
		 * @param x
		 * @param y
		 * @return true if the data has been changed, false otherwise
		 */
		public synchronized boolean startStroke( StrokeParams s, PenState ps) {
			if( data == null) 
				return false;
			stroke = s;
			
			strokeLayer = new BufferedImage( data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
			compositionLayer = new BufferedImage( data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int crgb = stroke.getColor().getRGB();
			
			prec = new LinkedList<>();
			oldState.x = ps.x;
			oldState.y = ps.y;
			oldState.pressure = ps.pressure;
			newState.x = ps.x;
			newState.y = ps.y;
			newState.pressure = ps.pressure;
			prec.add( ps);
			
			state = STATE.DRAWING;
			
			
			if( MUtil.coordInImage( ps.x, ps.y, strokeLayer) && strokeLayer.getRGB( ps.x, ps.y) != crgb) {
				strokeLayer.setRGB( ps.x, ps.y, crgb);
				return true;
			}
			return false;
		}
		
		/***
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
				prec.add( new PenState(newState));
				Graphics g = strokeLayer.getGraphics();

				Graphics2D g2 = (Graphics2D)g;
				g.setColor( stroke.getColor());
				g2.setStroke( new BasicStroke( 
						stroke.dynamics.getSize(newState)*stroke.width, 
						BasicStroke.CAP_ROUND, 
						BasicStroke.CAP_SQUARE));
				g2.drawLine( oldState.x, oldState.y, newState.x, newState.y);
				g.dispose();
				changed = true;
			}
			
			
			oldState.x = newState.x;
			oldState.y = newState.y;
			oldState.pressure = newState.pressure;
			return changed;
		}
		
		/***
		 * Updates the coordinates for the stroke
		 */
		public synchronized void updateStroke( PenState state) {
			newState.x = state.x;
			newState.y = state.y;
			newState.pressure = state.pressure;
		}
		
		public synchronized void endStroke() {
			state = STATE.READY;
			
			if( data != null) {
				BufferedImage img = workspace.checkoutImage(data);
				drawStrokeLayer(img.getGraphics());
				workspace.checkinImage(data);
			}
			
			strokeLayer.flush();
			compositionLayer.flush();
		}
		
		public PenState[] getHistory() {
			PenState[] array = new PenState[prec.size()];
			return prec.toArray(array);
		}

		public void drawStrokeLayer( Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			Composite c = g2.getComposite();
			switch( stroke.method) {
			case BASIC:
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

	// :::: Stroke Params
	public enum Method {BASIC, ERASE};
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
	}

	

	
	// :::: Other


	/***
	 * Simple queue-based flood fill.
	 * @return true if any changes were made
	 */
	public boolean fill( int x, int y, Color color, ImageData data) {
		if( data == null) return false;
		BufferedImage image = workspace.checkoutImage(data);
		if( image == null || !MUtil.coordInImage(x, y, image))  {
			workspace.checkinImage(data);
			return false;
		}
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add( MUtil.packInt(x, y));
		
		
		int w = image.getWidth();
		int h = image.getHeight();
		int bg = image.getRGB(x, y);
		int c = color.getRGB();
		
		if( bg == c) {
			workspace.checkinImage(data);
			return false;
		}
		
		while( !queue.isEmpty()) {
			int p = queue.poll();
			int ix = MUtil.low16(p);
			int iy = MUtil.high16(p);
			
			if( image.getRGB(ix, iy) != bg)
				continue;
				
			image.setRGB(ix, iy, c);

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
		
		workspace.checkinImage(data);
		return true;
	}
	
}
