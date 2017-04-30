package spirite.base.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;

/**
 * The RectPacker class contains algorithm(s) that take a list of Vec2is
 * and converts it into a PackedRect which contains a list of Rects 
 * such that none of them intersect and using various heuristics to minimize the
 * area which bounds all the Rects.
 *
 * @author Rory Burks
 */
public class RectanglePacker {
	
	public static class PackedRectangle {
		public int width;
		public int height;
		public List<Rect> packedRects = new ArrayList<>();
	}
	
	/**
	 * Tries to pack Rects using a quick-and-dirty algorithm modified from Sleator's 
	 * algorithm to take advantage of the excessive memory available to modern computers.  
	 * Once > half width boxes are stacked the algorithm just goes row-by-row and tries
	 *  to fit anything it can into the empty spaces (which are tracked and quickly analyzed
	 *  using a "space remaining" array similar to the Huang algorithm), stacking any 
	 *  Rect found to the right-most spot available.
	 */
	public static PackedRectangle modifiedSleatorAlgorithm( List<Vec2i> toPack) {
		// Remove bad Rects
		toPack.removeIf(new Predicate<Vec2i>() {
			@Override
			public boolean test(Vec2i d) {
				return (d==null) || (d.x <= 0 || d.y <= 0);
			}
		});
		
		// Wind the largest width amongst all Rects (the strip will have
		//	to be at least that wide)
		int minWidth = Collections.max(toPack, new Comparator<Vec2i>() {
			@Override
			public int compare(Vec2i o1, Vec2i o2) {
				return o1.x - o2.x;
			}
		}).x;
		
		// maxWidth and inc can be modified to effect the time spent finding
		//	the optimal Strip size (larget maxWidth and smaller inc mean more
		//	time spent, but more optimal fit)
		// inc should never be smaller than 1 and maxWidth should never be
		//	larger than the sum of all Vec2is' width
		//	(probably shouldn't be larger than the square root of the sum except 
		//	in weird cases)
		int maxWidth = minWidth*2;
		int inc = Math.round(Math.max(1.0f, (maxWidth-minWidth)/10.0f));
//		int maxWidth = 0;
//		for( Vec2i d : toPack) maxWidth += d.width;
//		int inc = 1;
		
		PackedRectangle ret = null;
		
		// Go through a set amount of widths to test and use the algorithm to
		//	pack the Rects, testing to see if the result is smaller in
		//	volume than the previous results.
		for( int width = minWidth; width < maxWidth; width += inc) {
			PackedRectangle pr = msaSub(toPack, width);
			if( ret == null || pr.width * pr.height < ret.width * ret.height) 
				ret = pr;
		}
		return ret;
	}
	private static PackedRectangle msaSub( List<Vec2i> toPack, int width) {
		// The field is essentially a 2D int array the size of the resulting
		//	strip.  Each value in the int array corresponds to how much free
		//	space is to the right of the position it represents (0 means the spot
		//	is currently occupied).
		// Because the height dynamically stretches whereas the width is fixed, 
		//	the vertical is the position of the outer Vector whereas the horizontal
		//	is the position of the inner Array
		ArrayList<int[]> field = new ArrayList<>();
		
		// Since we'll be doing a lot of arbitrary-index removing and the memory
		//	overhead is tiny compared to that of the field's memory consumption, 
		//	LinkedList will probably be better
		List<Vec2i> rects = new LinkedList<>(toPack);	
		
		int wy = 0;
		PackedRectangle ret = new PackedRectangle();
		

		// Step 0: Sort by non-increasing width
		rects.sort(new Comparator<Vec2i>() {
			@Override
			public int compare(Vec2i o1, Vec2i o2) {
				return o2.x - o1.x;
			}
		});
		
		// Step 1: for each Rect of width greater then half the strip width,
		//	stack them on top of each other
		Iterator<Vec2i> it = rects.iterator();
		while( it.hasNext()) {
			Vec2i d = it.next();
			if( d.x >= width/2) {
				int[] row = new int[width];
				for( int x = d.x; x < width; ++x) {
					row[x] = width - x;
				}
				
				field.add(row);	// Note: height guarenteed to be at least 1
				for( int y = 1; y < d.y; ++y) {
					field.add(row.clone());
				}
				
				ret.packedRects.add(new Rect(0,wy,d.x,d.y));
				wy += d.y;
				it.remove();
			}
			else break;
		}

		// Step 2 Sort by non-increasing height for reasons
		rects.sort(new Comparator<Vec2i>() {
			@Override
			public int compare(Vec2i o1, Vec2i o2) {
				return o2.y - o1.y;
			}
		});
		
		// Step 3: go row-by-row trying to fit anything that can into the empty 
		//	spaces.
		// Note: Because of our construction it's guaranteed that there are no
		//	"ceilings", i.e. Rects whose bottom is touching any air higher than
		//	y.
		int y = 0;
		while( !rects.isEmpty()) {
			if( field.size() <= y) {
				field.add(_newRow(width));
			}
			int[] row = field.get(y);
			
			
			int x = 0;
			while( x < width) {
				int space = row[x];
				if( space == 0) {
					++x;
				}
				else {
					it = rects.iterator();
					while( it.hasNext()) {
						Vec2i d = it.next();
						if( d.x <= row[x]) {
							// Puts it (the tallest box found that can fit) at the right-most 
							//	spot to minimize weird-looking areas.
							Rect rect = new Rect(x + row[x] - d.x,y,d.x,d.y);
							
							ret.packedRects.add(rect);
							it.remove();

							_addRect(ret, rect, field, width);
							break;
						}
					}
					x += space;
				}
			}
			++y;
		}
		
		ret.width = 0;
		ret.height = 0;
		for( Rect r : ret.packedRects) {
			if( r.width + r.x > ret.width) ret.width = r.width + r.x;
			if( r.height+ r.y > ret.height) ret.height= r.height+ r.y;
		}
		return ret;
	}
	
	private static int[] _newRow( int width) {
		int[] ret = new int[width];
		for( int i=0; i < width; ++i) {
			ret[i] = width-i;
		}
		return ret;
	}
	private static void _addRect(PackedRectangle pr, Rect rect, ArrayList<int[]> field, int width) {
		int[] buildRow = null;
		
		for( int y=rect.y; y < rect.height + rect.y; ++y) {
			if( y > field.size()) throw new IndexOutOfBoundsException();
			if( y == field.size()) {
				if( buildRow == null) {
					buildRow = _newRow(width);
					for( int x=0; x<rect.x; ++x) {
						buildRow[x] = rect.x-x;
					}
					for( int x=rect.x; x < rect.width + rect.x; ++x) {
						buildRow[x] = 0;
					}
					for( int x=rect.x + rect.width; x < width; ++x) {
						buildRow[x] = width - x;
					}
				}
				field.add(buildRow);
			}
			else {
				int[] row = field.get(y);
				for( int x = rect.x; x < rect.x + rect.width; ++x) {
					row[x] = 0;
				}
				for( int x = rect.x-1; x >= 0 && row[x] != 0; --x) {
					row[x] = rect.x-x;
				}
			}
		}
	}
	
	/** Tests to see if the PackedRectagle is poorly-described (either it has 
	 * intersections or has Rects that go outside of the packing bounds)
	 */
	public static boolean testBad(PackedRectangle pr) {
		for( Rect r1 : pr.packedRects) {
			if( r1.x < 0 || r1.y < 0 || r1.x + r1.width > pr.width 
				|| r1.y + r1.height > pr.height) {
				return true;
			}
			
			for( Rect r2 : pr.packedRects) {
				if( r1 != r2) {
					if( (new Rect(r1.x, r1.y, r1.width, r1.height)).intersects(
						new Rect(r2.x, r2.y, r2.width, r2.height))) {
						return true;
					}
				}
			}
		}
		return false;
	}
}