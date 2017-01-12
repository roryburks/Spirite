package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;

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
	BufferedImage working_image;
	Timer update_timer;
	StrokeEngine timedStroke = null;
	
	private enum STATE { READY, DRAWING };
	
	public DrawEngine( ) {
	}

	public StrokeEngine createStrokeEngine( ImageData data) {
		return new StrokeEngine(data);
	}
	
	/***
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
		
		List<Point> prec = new LinkedList<>();
		
		protected StrokeEngine(ImageData data) {
			this.data = data;
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
		public boolean startStroke( StrokeParams s, int x, int y) {
			if( data == null || data.getData() == null) 
				return false;
			stroke = s;
			BufferedImage img = data.getData();
			int crgb = stroke.getColor().getRGB();
			
			old_x = x;
			old_y = y;
			new_x = x;
			new_y = y;
			prec.add( new Point(x,y));
			
			state = STATE.DRAWING;
			
			
			if( MUtil.coordInImage( x, y, img) && img.getRGB(x, y) != crgb) {
				img.setRGB(x, y, crgb);
				return true;
			}
			return false;
		}
		
		/***
		 * 
		 * @return true if the step wan't a null-step (non-moving)
		 */
		public synchronized boolean stepStroke() {
			if( state != STATE.DRAWING || data == null || data.getData() == null) {
				MDebug.handleWarning( WarningType.STRUCTURAL, this, "Data Dropped mid-stroke (possible loss of Undo functionality)");
				endStroke();
				return false;
			}
			
			BufferedImage img = data.getData();
			boolean changed = false;
				
			// Draw Stroke (only if the mouse has moved)
			if( new_x != old_x || new_y != old_y) {
				prec.add( new Point(new_x,new_y));
				Graphics g = img.getGraphics();
				
				switch( stroke.method) {
				case BASIC:
					g.setColor( stroke.getColor());
					g.drawLine(old_x, old_y, new_x, new_y);
					break;
				case ERASE:
					Graphics2D g2 = (Graphics2D)g;
					Composite c = g2.getComposite()
							;
					g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
					g2.setColor( new Color(0,0,0,0));
					g2.drawLine( old_x, old_y, new_x, new_y);
					g2.setComposite( c);
				}
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
		
		public void endStroke() {
			old_x = -1;
			old_y = -1;
			state = STATE.READY;
			working_image = null;
		}
		
		public Point[] getHistory() {
			Point[] array = new Point[prec.size()];
			return prec.toArray(array);
		}
	}

	// :::: Stroke Params
	public enum Method {BASIC, ERASE};
	public static class StrokeParams {
		
		Color c = Color.BLACK;
		Method method = Method.BASIC;
		boolean locked = false;
		
		public StrokeParams() {}
		
		public void setColor( Color c) {
			if( !locked)
				this.c = c;
		}
		public Color getColor() {
			return c;
		}
		
		public void setMethod( Method method) {
			if( !locked)
				this.method = method;
		}
		public Method getMethod() {
			return method;
		}
		
	}

	

	
	// :::: Other


	/***
	 * Simple queue-based flood fill.
	 * @return true if any changes were made
	 */
	public boolean fill( int x, int y, Color color, ImageData data) {
		if( data == null) return false;
		BufferedImage image = data.getData();
		if( image == null) return false;
		
		if( !MUtil.coordInImage(x, y, image)) {
			return false;
		}
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add( MUtil.packInt(x, y));
		
		
		int w = image.getWidth();
		int h = image.getHeight();
		int bg = image.getRGB(x, y);
		int c = color.getRGB();
		
		if( bg == c) return false;
		
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
		
		return true;
	}
	
	// :::: Internal
	
}
