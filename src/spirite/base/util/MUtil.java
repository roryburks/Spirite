package spirite.base.util;

import java.awt.AlphaComposite;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.SwingUtilities;

import spirite.base.image_data.RawImage;
import spirite.base.util.ArrayInterpretation.IntCounter;
import spirite.base.util.ArrayInterpretation.InterpretedIntArray;
import spirite.base.util.DataCompaction.FloatCompactor;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.HybridHelper;

public class MUtil {
	
	public final static Point ORIGIN = new Point(0,0);


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
	public static Rect circumscribe( Rectangle rect1, Rectangle rect2) {
		return rectFromEndpoints(
				Math.min( rect1.x, rect2.x),
				Math.min( rect1.y, rect2.y),
				Math.max( rect1.x + rect1.width, rect2.x + rect2.width),
				Math.max( rect1.y + rect1.height, rect2.y + rect2.height)
				);
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
	/** Fills the image entirely with transparent data */
	public static void clearImage( BufferedImage image) {
		Graphics2D g2 = (Graphics2D)image.getGraphics();
		g2.setColor( new Color(0,0,0,0));
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC));
		g2.fillRect(0, 0, image.getWidth(), image.getHeight());
		g2.dispose();
	}
	
	public static boolean coordInImage( int x, int y, RawImage image) {
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
	public static ByteBuffer bufferFromImage( BufferedImage image) {
		// TODO: Figure out if there's ANY way to make this more efficient
		ByteBuffer byteBuffer;
		DataBuffer dataBuffer = image.getRaster().getDataBuffer();

		if (dataBuffer instanceof DataBufferInt) {
		    int[] pixelData = ((DataBufferInt) dataBuffer).getData();
		    
		    byteBuffer = ByteBuffer.allocate(pixelData.length * 4);
		    
		    for( int i=0; i < pixelData.length; i += 1) {
		    	// Change ARGB to RGBA
		    	int argb = pixelData[i];
		    	byteBuffer.put( (byte) ((argb >>> 16) & 0xFF));
		    	byteBuffer.put( (byte) ((argb >>> 8) & 0xFF));
		    	byteBuffer.put( (byte) ((argb >>> 0) & 0xFF));
		    	byteBuffer.put( (byte) ((argb >>> 24) & 0xFF));
		    }
		}
		else {
		    throw new IllegalArgumentException("Not implemented for data buffer type: " + dataBuffer.getClass());
		}
		
		byteBuffer.rewind();
		return byteBuffer;
	}

	/** Attempts to get an image from the clipboard, returning null if 
	 * there is no image or for some reason you can't get it. 
	 * @return
	 */
	public static BufferedImage imageFromClipboard() {
		try {
	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	
			// Convert Clipboard image into BufferedImage
			Image img = (Image)c.getData(DataFlavor.imageFlavor);
			

    		BufferedImage bi = new BufferedImage(  
    				img.getWidth(null), img.getHeight(null), HybridHelper.BI_FORMAT);
    		Graphics g = bi.getGraphics();
    		g.drawImage(img, 0, 0, null);
    		g.dispose();

    		return bi;
		} catch( IOException | UnsupportedFlavorException e) {}
		
		return null;
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
	 * 
	 */
	public static Rect findContentBounds( BufferedImage image, int buffer, boolean transparentOnly) 
			throws UnsupportedDataTypeException 
	{

		_ImageCropHelper data;
		
		int type = image.getType();
		switch( type) {
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
			data = new _ImageCropHelperByte(image);
			break;
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE:
			data = new _ImageCropHelperInt(image);
			break;
		default:
			throw new UnsupportedDataTypeException("Only programmed to deal with 4-byte color data.");
		}

		int x1 =0, y1=0, x2=0, y2=0;
				
		// Don't feel like going through and special-casing 1-size things.
		if( data.w <2 || data.h < 2) return new Rect(0,0,data.w,data.h);

		data.setBG(0, 0);

		// Usually the background color will be the top-left pixel, but
		//	sometimes it'll be the bottom-right pixel.
		// (Note all pixels in the edges share either a row or a column 
		//	with one of these two pixels, so it'll be one of the two).
		// Note: this also pulls double-duty of checking the special case
		//	of the 0th row and column, which simplifies the Binary Search
		if( !data.rowIsEmpty(0) || ! data.colIsEmpty(0))
			data.setBG(data.w-1, data.h-1);
		
		if(transparentOnly && !data.isBGTransparent())
			return new Rect(0,0,data.w,data.h);

		int ret;
		
		// Left
		ret = data.findFirstEmptyLine( new IntCounter(0,data.w-1), false);
		if( ret == -1) return new Rect(0,0,0,0);
		x1 = ret;
		
		// Right
		ret = data.findFirstEmptyLine( new IntCounter(data.w-1, 0), false);
		x2 = (ret == -1) ? data.w-1 : data.w-1-ret;

		// Top
		ret = data.findFirstEmptyLine( new IntCounter(0,data.h-1), true);
		if( ret == -1) return new Rect(0,0,0,0);
		y1 =  ret;

		// Bottom
		ret = data.findFirstEmptyLine( new IntCounter(data.h-1, 0), true);
		y2 = (ret == -1) ? data.h-1 : data.h-1-ret;

		x1 = Math.max(0, x1 - buffer);
		y1 = Math.max(0, y1 - buffer);
		x2 = Math.min(data.w-1, x2 + buffer);
		y2 = Math.min(data.h-1, y2 + buffer);
		
		
		return new Rect( x1, y1, x2-x1+1, y2-y1+1 );
	}
	private static abstract class _ImageCropHelper {
		final int w;
		final int h;
		
		_ImageCropHelper( BufferedImage image ) {
			this.w = image.getWidth();
			this.h = image.getHeight();
		}

		// Sanity checks would be inefficient, let's just assume everything
		//	works correctly because it's already been tested.
		abstract boolean verify( int x, int y);
		abstract void setBG( int x, int y);
		abstract boolean isBGTransparent();
		
		// Kind of Ugly code in here, but it's a bit much to copy all that
		// code just to swap X and Y and w for h
		
		/**
		 * @return true if the line contains only BG data
		 */
		private boolean lineIsEmpty( int num, boolean row) {
			if( row) {
				for(int x=0; x < w; ++x) {
					if( !verify(x,num))return false;
				}
			}
			else {
				for(int y=0; y < h; ++y) {
					if( !verify(num, y))return false;
				}
			}
			return true;
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
			
			for( int i=0; i < size; ++i) {
				if( !lineIsEmpty(data.get(i), row))
					return i;
			}
			return -1;
		}
	}
	private static class _ImageCropHelperByte extends _ImageCropHelper {
		final byte pixels[];
		final byte bgcolor[] = new byte[4];
		
		_ImageCropHelperByte(BufferedImage image) {
			super(image);
			this.pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		}

		@Override
		boolean verify(int x, int y) {
			int i = (x + y*w)*4;
			return pixels[i] == bgcolor[0] && 
					pixels[i+1] == bgcolor[1] && 
					pixels[i+2] == bgcolor[2] && 
					pixels[i+3] == bgcolor[3];
		}

		@Override
		void setBG(int x, int y) {
			int i = (x + y*w)*4;
			bgcolor[0] = pixels[i];
			bgcolor[1] = pixels[i+1];
			bgcolor[2] = pixels[i+2];
			bgcolor[3] = pixels[i+3];
		}

		@Override
		boolean isBGTransparent() {
			return bgcolor[0] == 0;
		}
	}
	private static class _ImageCropHelperInt extends _ImageCropHelper {
		final int pixels[];
		int bgcolor;
		
		_ImageCropHelperInt(BufferedImage image) {
			super(image);
			this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		}

		@Override
		boolean verify(int x, int y) {
			int i = (x + y*w);
			return pixels[i] == bgcolor;
		}

		@Override
		void setBG(int x, int y) {
			int i = (x + y*w);
			bgcolor = pixels[i];
		}

		@Override
		boolean isBGTransparent() {
			return ((bgcolor >>> 24) & 0xFF) == 0;
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
			if( toSend != null && toSend != reciever) {
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
	
	// Really seems like this should be a native class
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
