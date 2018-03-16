package spirite.base.util;


import spirite.base.graphics.IImage;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Transform;
import spirite.base.util.linear.Vec2;

import java.awt.*;
import java.awt.geom.PathIterator;
import java.util.Iterator;
import java.util.List;

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
	public static float distance( float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
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

	public static float lerp( float a, float b, float t) {
		return t*b + (1-t)*a;
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
	
	// ======
	// ==== Rectangle Functions
	/**
	 * Finds the bounds of a rectangle tranformed by a matrix
	 */
	public static Rect findBounds( Rect region, Transform matrix) {
		// Might be some slightly-more-clever way to determing this
		Vec2 p1 = matrix.apply(new Vec2(region.getX(), region.getY()));
		Vec2 p2 = matrix.apply(new Vec2(region.getX()+region.getWidth(), region.getY()));
		Vec2 p3 = matrix.apply(new Vec2(region.getX(), region.getY() + region.getHeight()));
		Vec2 p4 = matrix.apply(new Vec2(region.getX()+region.getWidth(), region.getY() + region.getHeight()));

		int x1 = (int)Math.floor( Math.min( Math.min( Math.min(p1.getX(), p2.getX()), p3.getX()), p4.getY()));
		int y1 = (int)Math.floor( Math.min( Math.min( Math.min(p1.getX(), p2.getX()), p3.getX()), p4.getY()));
		int x2 = (int)Math.ceil(Math.max( Math.max( Math.min(p1.getX(), p2.getX()), p3.getX()), p4.getY()));
		int y2 = (int)Math.ceil(Math.max( Math.max( Math.min(p1.getX(), p2.getX()), p3.getX()), p4.getY()));
		
		return new Rect( x1, y1, x2-x1, y2-y1);
	}
	
	/**
	 * Constructs a non-negative dimension Rectangle from two coordinates
	 */
	public static Rect rectFromEndpoints( int x1, int y1, int x2, int y2) {
		return new Rect( Math.min(x1, x2), Math.min(y1, y2),
				Math.abs(x1-x2), Math.abs(y1-y2));
	}
	/** Creates the smallest rectangle that contains all given points. */
	public static Rect rectFromPoints(List<Vec2> points) {
		if( points == null || points.size() == 0) return new Rect(0,0,0,0);
		int x1 = (int)Math.floor(points.get(0).getX());
		int y1 = (int)Math.floor(points.get(0).getY());
		int x2 = (int)Math.ceil(points.get(0).getX());
		int y2 = (int)Math.ceil(points.get(0).getY());
		
		Iterator<Vec2> it = points.iterator();
		it.next();	// Ignore 1 because we already did it
		while( it.hasNext()) {
			Vec2 p = it.next();
			int tx1 = (int)Math.floor(p.getX());
			int ty1 = (int)Math.floor(p.getY());
			int tx2 = (int)Math.ceil(p.getX());
			int ty2 = (int)Math.ceil(p.getY());
			if( tx1 < x1) x1 = tx1;
			if( ty1 < y1) y1 = ty1;
			if( tx2 < x2) x2 = tx2;
			if( ty2 < y2) y2 = ty2;
		}
		
		return new Rect( x1, y1, x2-x1, y2-y1);
	}
	
	/** Stretches the Rectangle from the center by a given scaler */
	public static Rect scaleRect( Rect cropSection, float scalar) {
		return new Rect(
				cropSection.getX() - Math.round(cropSection.getWidth() * (scalar-1)/2.0f),
				cropSection.getY() - Math.round(cropSection.getHeight() * (scalar-1)/2.0f),
				Math.round(cropSection.getWidth() * scalar),
				Math.round(cropSection.getHeight() * scalar)
			);
		
	}
	
	/** Returns the smallest rectangle such that rect1 and rect2 are contained
	 * within it.	 */
	public static Rect circumscribe( Rect rect1, Rect rect2) {
		return rectFromEndpoints(
				Math.min( rect1.getX(), rect2.getX()),
				Math.min( rect1.getY(), rect2.getY()),
				Math.max( rect1.getX() + rect1.getWidth(), rect2.getX() + rect2.getWidth()),
				Math.max( rect1.getY() + rect1.getHeight(), rect2.getY() + rect2.getHeight())
				);
	}
	
	public static Rect circumscribeTrans( Rect oldRect, Transform trans) {
		Vec2 p1 =  trans.apply( new Vec2(oldRect.getX(), oldRect.getY()));
		Vec2 p2 =  trans.apply( new Vec2(oldRect.getX()+oldRect.getWidth(), oldRect.getY()));
		Vec2 p3 =  trans.apply( new Vec2(oldRect.getX(), oldRect.getY()+oldRect.getHeight()));
		Vec2 p4 =  trans.apply( new Vec2(oldRect.getX()+oldRect.getWidth(), oldRect.getY()+oldRect.getHeight()));

		int x1 = (int)Math.min( Math.floor(p1.getX()), Math.min(Math.floor(p2.getX()), Math.min(Math.floor(p3.getX()), Math.floor(p4.getX()))));
		int y1 = (int)Math.min( Math.floor(p1.getY()), Math.min(Math.floor(p2.getY()), Math.min(Math.floor(p3.getY()), Math.floor(p4.getY()))));
		int x2 = (int)Math.max( Math.ceil(p1.getX()), Math.max(Math.ceil(p2.getX()), Math.max(Math.ceil(p3.getX()), Math.ceil(p4.getX()))));
		int y2 = (int)Math.max( Math.ceil(p1.getY()), Math.max(Math.ceil(p2.getY()), Math.max(Math.ceil(p3.getY()), Math.ceil(p4.getY()))));
		
		return rectFromEndpoints(x1,y1,x2,y2);
	}
	
	// ==========
	// ==== Vector Functions
	/** Converts a line segment and a point into two coordinates: the first representing 
	 * its projection onto the line segment normalized such that t=0 means it's perpendicular
	 * to (x1,y1) and t=1 for (x2,y2).  The second representing the distance from the line 
	 * extended from the line segment
	 */
	public static Vec2 projectOnto(float x1, float y1, float x2, float y2, Vec2 p) {

		Vec2 b = new Vec2(x2 - x1, y2 - y1);
		float scale_b = b.getMag();
		float scale_b2 = scale_b*scale_b;
		
		Vec2 a = new Vec2(p.getX() - x1, p.getY() - y1);
		
		float t =  a.dot(b) / scale_b2;	// the extra / ||b|| is to normalize it to ||b|| = 1
		float m = a.cross(b) / scale_b;
		
		return new Vec2(t,m);
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