package spirite.draw_engine;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import spirite.MUtil;
import spirite.brains.MasterControl;

/***
 * Pretty much anything which alters the image data goes through the DrawEngine
 * 
 * @author Rory Burks
 *
 */
public class DrawEngine {
	int old_x, old_y;
	int new_x, new_y;
	MasterControl master;
	StrokeParams stroke;
	
	BufferedImage working_image;
	Timer update_timer;
	
	private enum STATE { READY, DRAWING };
	STATE state = STATE.READY;
	
	public DrawEngine( MasterControl master) {
		this.master = master;
		stroke = null;
		
		update_timer = new Timer();
		update_timer.scheduleAtFixedRate( new TimerTask() {
			@Override
			public void run() {
				 stepStroke();
			}
			
		}, 100, 16);
	}
	
	// :::: Stroke engine
	private synchronized void stepStroke() {
		if( state != STATE.DRAWING)
			return;
		
		working_image = master.getImageManager().getActivePart().getData();
		
		if( working_image == null ) {
			System.out.println("s");
			endStroke();
			return;
		}
		
//		System.out.println( old_x + "," + old_y + " : " + new_x + "," + new_y + "::: " + state);
		
			
		// Draw Stroke
		Graphics g = working_image.getGraphics();
		
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
		
		// Refresh
		master.refreshImage();
		
		old_x = new_x;
		old_y = new_y;
	}
	
	// :::: Stroke API
	public void startStroke( StrokeParams s, int x, int y) {
		System.out.println("Start");
		old_x = x;
		old_y = y;
		new_x = x;
		new_y = y;
		
		stroke = s;
		
		if( MUtil.coordInImage( x, y, working_image)) {
			working_image.setRGB(x, y, stroke.getColor().getRGB());
			master.refreshImage();
		}
		
		state = STATE.DRAWING;
	}
	
	public synchronized void updateStroke( int x, int y) {
		new_x = x;
		new_y = y;
	}
	
	public void endStroke() {
		old_x = -1;
		old_y = -1;
		state = STATE.READY;
		working_image = null;
//		System.out.println("ASFASDF");
	}
	
	// :::: Other
	
	// Simple Queue flood fill
	public void fill( int x, int y, Color color) {
		BufferedImage image = master.getImageManager().getActivePart().getData();
		if( image == null) return;
		
		if( !MUtil.coordInImage(x, y, image)) {
			return;
		}
		
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add( MUtil.packInt(x, y));
		
		
		int w = image.getWidth();
		int h = image.getHeight();
		int bg = image.getRGB(x, y);
		int c = color.getRGB();
		
		if( bg == c) return;
		
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
			if( ix - 1 > 0) {
				queue.add( MUtil.packInt(ix-1, iy));
			}
			if( iy + 1 < h) {
				queue.add( MUtil.packInt(ix, iy+1));
			}
			if( iy - 1 > 0) {
				queue.add( MUtil.packInt(ix, iy-1));
			}
		}
		
		master.refreshImage();
	}
	
	// :::: Internal
	
	// :::: Stroke Params
	public static class StrokeParams {
		public enum Method {BASIC, ERASE};
		
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
		
		
	}
}
