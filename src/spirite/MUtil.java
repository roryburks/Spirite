package spirite;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.SwingUtilities;

import mutil.ArrayInterpretation.IntCounter;
import mutil.ArrayInterpretation.InterpretedIntArray;

public class MUtil {

	// :::: Math Functions
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
	public static float clip( float min, float value, float max) {
		if( value < min) return min;
		if( value > max) return max;
		return value;
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
	 * Constructs a non-negative dimension Rectangle from two coordinates
	 */
	public static Rectangle rectFromEndpoints( int x1, int y1, int x2, int y2) {
		return new Rectangle( Math.min(x1, x2), Math.min(y1, y2),
				Math.abs(x1-x2), Math.abs(y1-y2));
	}
	
	/** Stretches the Rectangle from the center by a given scaler */
	public static Rectangle scaleRect( Rectangle rect, float scalar) {
		return new Rectangle(
				rect.x - Math.round(rect.width * (scalar-1)/2.0f),
				rect.y - Math.round(rect.height * (scalar-1)/2.0f),
				Math.round(rect.width * scalar),
				Math.round(rect.height * scalar)
			);
		
	}
	
	/** Returns the smallest rectangle such that rect1 and rect2 are contained
	 * within it.	 */
	public static Rectangle circumscribe( Rectangle rect1, Rectangle rect2) {
		return rectFromEndpoints(
				Math.min( rect1.x, rect2.x),
				Math.min( rect1.y, rect2.y),
				Math.max( rect1.x + rect1.width, rect2.x + rect2.width),
				Math.max( rect1.y + rect1.height, rect2.y + rect2.height)
				);
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
	
	// :::: Other
	/** Fills the image entirely with transparent data */
	public static void clearImage( BufferedImage image) {
		Graphics2D g2 = (Graphics2D)image.getGraphics();
		g2.setColor( new Color(0,0,0,0));
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC));
		g2.fillRect(0, 0, image.getWidth(), image.getHeight());
		g2.dispose();
	}
	
	public static boolean coordInImage( int x, int y, BufferedImage image) {
		if( image == null) return false;
		
		if( x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) 
			return false;
		
		return true;
	}
	
	public static BufferedImage deepCopy( BufferedImage toCopy) {
		return new BufferedImage( 
				toCopy.getColorModel(),
				toCopy.copyData(null),
				toCopy.isAlphaPremultiplied(),
				null);
	}
	
	/**
	 * 	Returns a rectangle that represents the sub-section of the original image 
	 * such that as large full rectangular regions on the outside of a single
	 * color are removed without touching non-background data.
	 * 
	 * @param image
	 * 		The image to crop.
	 * @param buffer 
	 * 		The amount of pixels outside of the region with non-background
	 * 		content to preserve on each side of the image.
	 * @param transparentOnly
	 * 		If true, it only crops if the background is transparent, if 
	 * 		false it will crop any background color.
	 * @return
	 * 		The Rectangle of the image as it should be cropped.
	 * @throws 
	 * 		UnsupportedDataTypeException if the ColorModel does not conform
	 * 		to the a supported format
	 */
	public static Rectangle findContentBounds( BufferedImage image, int buffer, boolean transparentOnly) throws UnsupportedDataTypeException {
		_ImageCropHelper data = new _ImageCropHelper(image);
		int x1 =0, y1=0, x2=0, y2=0;
				
		// Don't feel like going through and special-casing 1-size things.
		if( data.w <2 || data.h < 2) return new Rectangle(0,0,data.w,data.h);
		
		int lpp = image.getColorModel().getPixelSize();
		if( lpp != 4*8) throw new UnsupportedDataTypeException("Only programmed to deal with 4-byte color data.");

		data.bgcolor = data.pixels[0];

		// Usually the background color will be the top-left pixel, but
		//	sometimes it'll be the bottom-right pixel.
		// (Note all pixels in the edges share either a row or a column 
		//	with one of these two pixels, so it'll be one of the two).
		// Note: this also pulls double-duty of checking the special case
		//	of the 0th row and column, which simplifies the Binary Search
		if( !data.rowIsEmpty(0) || ! data.colIsEmpty(0)) {
			int i = data.h*data.w - 1;
			data.bgcolor = data.pixels[i];
		}
		
		if(transparentOnly && ((data.bgcolor >>> 24) & 0xFF) != 0) 
			return new Rectangle(0,0,data.w,data.h);
		
		// Find Left-most 
		IntCounter c = new IntCounter( 0, data.w-1);

		int ret;
		
		// Left
		ret = data.findFirstEmptyLine( new IntCounter(0,data.w-1), false);
		x1 = (ret == -1) ? 0 : ret;
		
		// Right
		ret = data.findFirstEmptyLine( new IntCounter(data.w-1, 0), false);
		x2 = (ret == -1) ? data.w-1 : data.w-1-ret;

		// Top
		ret = data.findFirstEmptyLine( new IntCounter(0,data.h-1), true);
		y1 = (ret == -1) ? 0 : ret;

		// Bottom
		ret = data.findFirstEmptyLine( new IntCounter(data.h-1, 0), true);
		y2 = (ret == -1) ? data.h-1 : data.h-1-ret;

		x1 = Math.max(0, x1 - buffer);
		y1 = Math.max(0, y1 - buffer);
		x2 = Math.min(data.w-1, x2 + buffer);
		y2 = Math.min(data.h-1, y2 + buffer);
		
		
		return new Rectangle( x1, y1, x2-x1+1, y2-y1+1 );
	}
	private static class _ImageCropHelper {
		final int w;
		final int h;
		final boolean hcheck[];
		final boolean vcheck[];
		final int pixels[];
		int bgcolor;
		
		private boolean transpose = false;
		
		_ImageCropHelper( BufferedImage image ) {
			this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			this.w = image.getWidth();
			this.h = image.getHeight();
			this.hcheck = new boolean[w];
			this.vcheck = new boolean[h];
		}
		
		// Sanity checks would be inefficient, let's just assume everything
		//	works correctly because it's already been tested.
		boolean compare( int x1, int y1, int x2, int y2) {
			int i1 = x1 + y1*w;
			int i2 = x2 + y2*w;
			return (pixels[i1] == pixels[i2]);
		}
		boolean verify( int x, int y) {
			int i = (transpose)?(y+x*w):(x + y*w);
			return (pixels[i] == bgcolor);
		}
		
		// Kind of Ugly code in here, but it's a bit much to copy all that
		// code just to swap X and Y and w for h
		
		/**
		 * @return true if the line contains only BG data
		 */
		private boolean lineIsEmpty( int num, boolean row) {
			Queue<Integer> queue = new LinkedList<Integer>();

			try {
				if( row) {
					queue.add(MUtil.packInt(w,0));
					transpose = false;
				}
				else {
					queue.add(MUtil.packInt(h,0));
					transpose = true;
				}
				if(!verify( 0, num))
					return false;
				while( !queue.isEmpty()) {
					int p = queue.poll();
					int hi = high16(p);
					int lo = low16(p);
					
					int n = (hi + lo) / 2;
					
					
					if( !verify( n, num))
						return false;

					if( hi-n > 1) queue.add(packInt(hi,n));
					if( n-lo > 1) queue.add(packInt(n,lo));
				}
				
				return true;
			}finally {
				transpose = false;
			}
		}
		boolean rowIsEmpty( int rownum) {
			return lineIsEmpty(rownum, true);
		}
		boolean colIsEmpty( int colnum) {
			return lineIsEmpty(colnum, false);
		}
		
		private int findFirstEmptyLine( InterpretedIntArray data, boolean row) {
			int size = data.length();
			if( size == 0) return -1;
			
			int rightmostFree = -1;
			int leftmostBlocked = Integer.MAX_VALUE;
			
			Queue<Integer> queue = new LinkedList<>();
			queue.add( MUtil.packInt(size,0));
			
			boolean zeroth = lineIsEmpty(data.get(0), row);
			
			if( !zeroth) return -1;
			
			while( !queue.isEmpty()) {
				int p = queue.poll();
				int hi = MUtil.high16(p);
				int lo = MUtil.low16(p);
				 
				int n = (hi + lo) / 2;
				
				if( lineIsEmpty(data.get(n), row)) {
					if( n < leftmostBlocked && n > rightmostFree)
						rightmostFree = n;
					
					if( hi-n > 1) queue.add(MUtil.packInt(hi,n));
				}
				else if( n < leftmostBlocked) {
					leftmostBlocked = n;
					if( rightmostFree > leftmostBlocked)
						rightmostFree = -1;
				}
				if( n-lo > 1) queue.add( MUtil.packInt(n,lo));
			}
			
			
			//The only way you could be at this point is if the zeroth
			//	row is free (otherwise it'd return -1), so the rightmostFree
			//	must at least be 0.
			return (rightmostFree == -1) ? 0 : rightmostFree;
		}
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
	
	// Really seems like this should be a native function
	public static class TransferableImage implements Transferable {
		private Image image;
		public TransferableImage( Image image) {this.image = image;}
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if( isDataFlavorSupported( flavor))
				return image;
			else
				throw new UnsupportedFlavorException(flavor);
		}
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor == DataFlavor.imageFlavor;
		}		
	}
}
