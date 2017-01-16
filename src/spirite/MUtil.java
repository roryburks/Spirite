package spirite;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public class MUtil {

	// :::: Math Functions
	public static int packInt( int low, int high) {
		return (low&0xffff) | ((high&0xffff) << 16);
	}
	
	public static int low16( int i) {
		return i & 0xffff;
	}
	
	public static int high16( int i) {
		return i >>> 16;
	}
	
	public static double distance( double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
	}

	/** 
	 * Places t in between start and end such that it is offset by an integer
	 * number of rotations of start to end. <br>
	 * (ex: if start = 10, end = 20, t = 65, returns 15)
	 */
	public static float cycle( float start, float end, float t) {
		float diff = end - start;
		if( diff == 0.0f)
			return 0.0f;
		
		return ((t - start) % diff + diff) % diff + start;
	}

	/** 
	 * Places t in between start and end such that it is offset by an integer
	 * number of rotations of start to end. <br>
	 * (ex: if start = 10, end = 20, t = 65, returns 15)
	 */
	public static int cycle( int start, int end, int t) {
		int diff = end - start;
		if( diff == 0)
			return 0;
		
		return ((((t - start) % diff) + diff) % diff) + start;
	}
	
	// ::::
	public static List<Integer> arrayToList( int arr[]) {
		List<Integer> list = new ArrayList<>(arr.length);
		
		for( int i = 0; i < arr.length; ++i) {
			list.add(arr[i]);
		}
		return list;
	}
	
	// :::: String
	
	// :::: Other
	/** Fills the image entirely with transparent data */
	public static void clearImage( BufferedImage image) {
		Graphics2D g2 = (Graphics2D)image.getGraphics();
		g2.setColor( new Color(0,0,0,0));
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC));
		g2.fillRect(0, 0, image.getWidth(), image.getHeight());
	}
	
	public static boolean coordInImage( int x, int y, BufferedImage image) {
		if( image == null) return false;
		
		if( x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) 
			return false;
		
		return true;
	}
	
	/***
	 * Called when an overlaying component (such as a GlassPane) eats a mouse event, but
	 * still wants the components bellow to receive it.
	 */
	public static void redispatchMouseEvent( Component reciever, Component container, MouseEvent evt) {
		Point p = SwingUtilities.convertPoint(reciever, evt.getPoint(), container);
		
		if( p.y < 0) { 
			// Not in component
		} else {
			Component toSend = 
					SwingUtilities.getDeepestComponentAt(container, p.x, p.y);
			if( toSend != null) {
				Point convertedPoint = SwingUtilities.convertPoint(container, p, toSend);
				toSend.dispatchEvent( new MouseEvent(
						toSend,
						evt.getID(),
						evt.getWhen(),
						evt.getModifiers(),
						convertedPoint.x,
						convertedPoint.y,
						evt.getClickCount(),
						evt.isPopupTrigger()
						));
			}
			else {
			}
		}
	}
}
