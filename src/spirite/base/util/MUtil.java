package spirite.base.util;


import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Iterator;

import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;

public class MUtil {


    public static String joinString( CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        StringBuilder builder = new StringBuilder();
        Iterator<? extends CharSequence> it = elements.iterator();
        while( it.hasNext()) {
            builder.append( it.next());
            if( it.hasNext())
                builder.append(delimiter);
        }
        return builder.toString();
    }
    
	// ==============
	// ==== Math Functions
	public static int packInt( int high, int low) {
		return ((high&0xffff) << 16) | (low&0xffff);
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

	public static int clip( int min, int value, int max) {
		if( value < min) return min;
		if( value > max) return max;
		return value;
	}
	public static double clip( double min, double value, double max) {
		if( value < min) return min;
		if( value > max) return max;
		return value;
	}
	public static float clip( float min, float value, float max) {
		if( value < min) return min;
		if( value > max) return max;
		return value;
	}
	
	public static double lerp( double a, double b, double t) {
		return t*b + (1-t)*a;
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
	
	/**
	 * Finds the bounds of a rectangle tranformed by a matrix
	 */
	public static Rect findBounds( Rect region, MatTrans matrix) {
		// Might be some slightly-more-clever way to determing this
		Vec2 p1 = matrix.transform(new Vec2(region.x, region.y),  new Vec2());
		Vec2 p2 = matrix.transform(new Vec2(region.x+region.width, region.y),  new Vec2());
		Vec2 p3 = matrix.transform(new Vec2(region.x, region.y + region.height),  new Vec2());
		Vec2 p4 = matrix.transform(new Vec2(region.x+region.width, region.y + region.height),  new Vec2());

		int x1 = (int)Math.floor( Math.min( Math.min( Math.min(p1.x, p2.x), p3.x), p4.y));
		int y1 = (int)Math.floor( Math.min( Math.min( Math.min(p1.x, p2.x), p3.x), p4.y));
		int x2 = (int)Math.ceil(Math.max( Math.max( Math.min(p1.x, p2.x), p3.x), p4.y));
		int y2 = (int)Math.ceil(Math.max( Math.max( Math.min(p1.x, p2.x), p3.x), p4.y));
		
		return new Rect( x1, y1, x2-x1, y2-y1);
	}
	
	/**
	 * Constructs a non-negative dimension Rectangle from two coordinates
	 */
	public static Rect rectFromEndpoints( int x1, int y1, int x2, int y2) {
		return new Rect( Math.min(x1, x2), Math.min(y1, y2),
				Math.abs(x1-x2), Math.abs(y1-y2));
	}
	
	/** Stretches the Rectangle from the center by a given scaler */
	public static Rect scaleRect( Rect cropSection, float scalar) {
		return new Rect(
				cropSection.x - Math.round(cropSection.width * (scalar-1)/2.0f),
				cropSection.y - Math.round(cropSection.height * (scalar-1)/2.0f),
				Math.round(cropSection.width * scalar),
				Math.round(cropSection.height * scalar)
			);
		
	}
	
	/** Returns the smallest rectangle such that rect1 and rect2 are contained
	 * within it.	 */
	public static Rect circumscribe( Rect rect1, Rect rect2) {
		return rectFromEndpoints(
				Math.min( rect1.x, rect2.x),
				Math.min( rect1.y, rect2.y),
				Math.max( rect1.x + rect1.width, rect2.x + rect2.width),
				Math.max( rect1.y + rect1.height, rect2.y + rect2.height)
				);
	}
	
	public static Rect circumscribeTrans( Rect oldRect, MatTrans trans) {
		Vec2 p1 =  trans.transform( new Vec2(oldRect.x, oldRect.y), new Vec2());
		Vec2 p2 =  trans.transform( new Vec2(oldRect.x+oldRect.width, oldRect.y), new Vec2());
		Vec2 p3 =  trans.transform( new Vec2(oldRect.x, oldRect.y+oldRect.height), new Vec2());
		Vec2 p4 =  trans.transform( new Vec2(oldRect.x+oldRect.width, oldRect.y+oldRect.height), new Vec2());

		int x1 = (int)Math.min( Math.floor(p1.x), Math.min(Math.floor(p2.x), Math.min(Math.floor(p3.x), Math.floor(p4.x))));
		int y1 = (int)Math.min( Math.floor(p1.y), Math.min(Math.floor(p2.y), Math.min(Math.floor(p3.y), Math.floor(p4.y))));
		int x2 = (int)Math.max( Math.ceil(p1.x), Math.max(Math.ceil(p2.x), Math.max(Math.ceil(p3.x), Math.ceil(p4.x))));
		int y2 = (int)Math.max( Math.ceil(p1.y), Math.max(Math.ceil(p2.y), Math.max(Math.ceil(p3.y), Math.ceil(p4.y))));
		
		return rectFromEndpoints(x1,y1,x2,y2);
	}
	
	// :::: String
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	// :::: Image-related
	
	public static boolean coordInImage( int x, int y, IImage image) {
		if( image == null) return false;
		
		if( x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) 
			return false;
		
		return true;
	}
	
	
	/** Fills the supplied FloatCompactors with points representing the path 
	 * along the Shape's PathIterator with flatness.	 */
	public static void shapeToPoints( 
			Shape shape, 
			FloatCompactor x_, 
			FloatCompactor y_, 
			double flatness, 
			boolean closed)
	{
		PathIterator pi = shape.getPathIterator(null, 1);
		float coords[] = new float[6];
		
		int startx = x_.size();
		int starty = y_.size();
		
		while( !pi.isDone()) {
			int res = pi.currentSegment(coords);
			switch( res) {
			case PathIterator.SEG_LINETO:
				x_.add(coords[0]);
				y_.add(coords[1]);
				break;
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_CLOSE:break;
			}
			pi.next();
		}
		if( closed) {
			x_.add(x_.get(startx));
			y_.add(y_.get(starty));	
		}
	}
}
