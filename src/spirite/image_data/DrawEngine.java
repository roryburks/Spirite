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
		int old_x, old_y;
		int new_x, new_y;
		STATE state = STATE.READY;

		StrokeParams stroke;
		ImageData data;
		BufferedImage strokeLayer;
		BufferedImage compositionLayer;
		
		List<Point> prec = new LinkedList<>();
		
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
		public synchronized boolean startStroke( StrokeParams s, int x, int y) {
			if( data == null) 
				return false;
			stroke = s;
			
			strokeLayer = new BufferedImage( data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
			compositionLayer = new BufferedImage( data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int crgb = stroke.getColor().getRGB();
			
			prec = new LinkedList<>();
			old_x = x;
			old_y = y;
			new_x = x;
			new_y = y;
			prec.add( new Point(x,y));
			
			state = STATE.DRAWING;
			
			
			if( MUtil.coordInImage( x, y, strokeLayer) && strokeLayer.getRGB(x, y) != crgb) {
				strokeLayer.setRGB(x, y, crgb);
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
			if( new_x != old_x || new_y != old_y) {
				prec.add( new Point(new_x,new_y));
				Graphics g = strokeLayer.getGraphics();

				Graphics2D g2 = (Graphics2D)g;
				g.setColor( stroke.getColor());
				g2.setStroke( new BasicStroke( stroke.width, BasicStroke.CAP_ROUND, BasicStroke.CAP_SQUARE));
				g2.drawLine( old_x, old_y, new_x, new_y);
				g.dispose();
				changed = true;
			}
			
			
			old_x = new_x;
			old_y = new_y;
			return changed;
		}
		
		/***
		 * Updates the coordinates for the stroke
		 */
		public synchronized void updateStroke( int x, int y) {
			new_x = x;
			new_y = y;
		}
		
		public synchronized void endStroke() {
			state = STATE.READY;
			old_x = -1;
			old_y = -1;
			if( data != null) {
				BufferedImage img = workspace.checkoutImage(data);
				drawStrokeLayer(img.getGraphics());
				workspace.checkinImage(data);
			}
			
			strokeLayer.flush();
			compositionLayer.flush();
		}
		
		public Point[] getHistory() {
			Point[] array = new Point[prec.size()];
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
