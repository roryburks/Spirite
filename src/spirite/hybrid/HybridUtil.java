package spirite.hybrid;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;

import javax.activation.UnsupportedDataTypeException;
import javax.imageio.ImageIO;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.GLImage;
import spirite.base.util.ArrayInterpretation.IntCounter;
import spirite.base.util.ArrayInterpretation.InterpretedIntArray;
import spirite.base.util.glu.GLC;
import spirite.base.util.linear.Rect;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.PCUtil;
import spirite.pc.graphics.ImageBI;
import spirite.pc.jogl.JOGLCore;

public class HybridUtil {
	public static class UnsupportedImageTypeException extends Exception {
		private UnsupportedImageTypeException(String message) {super(message);}
	}
	

	/**
	 * Converts a RawImage from one type to another.  If it already is that type
	 * of RawImage, returns it unchanged.
	 * <br><br>
	 * Supported Conversions:
	 *  <li>ImageBI -> GLImage
	 * 	<li>GLImage -> ImageBI
	 * @param from Image to Convert
	 * @param to Class to convert it to
	 * @see HybridHelper.loadImageIntoGL
	 * */
	public static <T extends RawImage> T convert( RawImage from, Class<? extends T> to) {
		if( from.getClass() == to)
			return (T)from;
		
		if( to == GLImage.class) {
			GL2 gl = JOGLCore.getGL2();
			
			int result[] = new int[1];
			gl.glGenTextures(1, result, 0);
			int tex = result[0];
	        gl.glBindTexture( GLC.GL_TEXTURE_2D, tex);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MIN_FILTER,GLC.GL_NEAREST);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MAG_FILTER,GLC.GL_NEAREST);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_S,GLC.GL_CLAMP_TO_EDGE);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_T,GLC.GL_CLAMP_TO_EDGE);
	        HybridHelper.loadImageIntoGL( from, gl);
	        return (T)new GLImage( tex, from.getWidth(), from.getHeight(), false);
		}
		if( to == ImageBI.class) {
			if( from.getClass() == GLImage.class) {
				return (T)new ImageBI(PCUtil.glToBI((GLImage)from));
			}
		}
		
		MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unsupported Conversion (in HybridUtil).\nFrom:"+from.getClass() +"\nTo:" +to);
		
		return null;
	}
	
	/**
	 * Converts a RawImage from one type to another.  If it already is that type
	 * of RawImage, returns it unchanged.
	 * <br><br>
	 * Supported Conversions:
	 *  <li>ImageBI -> GLImage
	 * 	<li>GLImage -> ImageBI
	 * @param from Image to Convert
	 * @param to Class to convert it to
	 * @see HybridHelper.loadImageIntoGL
	 * */
	public static IImage convert( IImage from, Class<? extends RawImage> to) {
		if( from.getClass() == to)
			return from;
		
		if( to == GLImage.class) {
			GL2 gl = JOGLCore.getGL2();
			
			int result[] = new int[1];
			gl.glGenTextures(1, result, 0);
			int tex = result[0];
	        gl.glBindTexture( GLC.GL_TEXTURE_2D, tex);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MIN_FILTER,GLC.GL_NEAREST);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MAG_FILTER,GLC.GL_NEAREST);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_S,GLC.GL_CLAMP_TO_EDGE);
	        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_T,GLC.GL_CLAMP_TO_EDGE);
	        HybridHelper.loadImageIntoGL( from, gl);
	        return new GLImage( tex, from.getWidth(), from.getHeight(), false);
		}
		if( to == ImageBI.class) {
			if( from.getClass() == GLImage.class) {
				return new ImageBI(PCUtil.glToBI((GLImage)from));
			}
		}
		
		MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unsupported Conversion (in HybridUtil).\nFrom:"+from.getClass() +"\nTo:" +to);
		
		return null;
	}
	
	// =======
	// ==== IO
	public static RawImage load( File f) throws IOException {
		BufferedImage bi = ImageIO.read(f);
		RawImage img = HybridHelper.createImage(bi.getWidth(), bi.getHeight());
		GraphicsContext gc = img.getGraphics();
		gc.clear();
		gc.drawImage( new ImageBI(bi), 0, 0);
		gc.dispose();
		
		return img;
	}

	public static RawImage load(ByteArrayInputStream byteArrayInputStream) throws IOException {
		BufferedImage bi = ImageIO.read(byteArrayInputStream);
		RawImage img = HybridHelper.createImage(bi.getWidth(), bi.getHeight());
		GraphicsContext gc = img.getGraphics();
		gc.clear();
		gc.drawImage( new ImageBI(bi), 0, 0);
		gc.dispose();
		
		return img;
	}
	
	public static void savePNG( IImage raw, OutputStream os) throws IOException {
		ImageIO.write( ((ImageBI)HybridUtil.convert(raw, ImageBI.class)).img, "png", os);
	}
	
	public static RawImage copyForSaving( IImage raw) {
		if( raw instanceof ImageBI)
			return raw.deepCopy();
		else 
			return (ImageBI)HybridUtil.convert(raw, ImageBI.class);
	}

	public static void saveEXT(RawImage raw, String ext, File f) throws IOException {
		ImageIO.write( ((ImageBI)HybridUtil.convert(raw, ImageBI.class)).img, ext, f);
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
	public static Rect findContentBounds( IImage raw, int buffer, boolean transparentOnly) 
			throws UnsupportedImageTypeException 
	{
		_ImageCropHelper data;
		
		if( raw instanceof ImageBI) {
			BufferedImage image = ((ImageBI)raw).img;

			
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
				throw new UnsupportedImageTypeException("Only programmed to deal with 4-byte color data.");
			}
		}
		else if( raw instanceof GLImage) {
			GLImage img = (GLImage)raw;
			GLEngine engine = GLEngine.getInstance();
			GL2 gl = engine.getGL2();
			int w = img.getWidth();
			int h = img.getHeight();
			
			engine.setTarget(img);

			IntBuffer read = IntBuffer.allocate(w*h);
			gl.glReadnPixels( 0, 0, w, h, 
					GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, 4*w*h, read);
			
			data = new _ImageCropHelperInt(read.array(), w, h);
		}
		else 
			throw new UnsupportedImageTypeException("Unsupported RawImage type");


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
		
		AtomicReference<Integer> leftRet = new AtomicReference<Integer>(null);
		Thread left = new Thread(() -> {
			leftRet.set(data.findFirstEmptyLine( new IntCounter(0,data.w-1), false));
		});
		AtomicReference<Integer> rightRet = new AtomicReference<Integer>(null);
		Thread right = new Thread(() -> {
			rightRet.set(data.findFirstEmptyLine( new IntCounter(data.w-1, 0), false));
		});
		AtomicReference<Integer> topRet = new AtomicReference<Integer>(null);
		Thread top = new Thread(() -> {
			topRet.set(data.findFirstEmptyLine( new IntCounter(0,data.h-1), true));
		});
		AtomicReference<Integer> bottomRet = new AtomicReference<Integer>(null);
		Thread bottom = new Thread(() -> {
			bottomRet.set(data.findFirstEmptyLine( new IntCounter(data.h-1, 0), true));
		});
		
		left.start();
		right.start();
		top.start();
		bottom.start();
		try {
			left.join();
			right.join();
			top.join();
			bottom.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if( leftRet.get() == -1)
			return new Rect(0,0,0,0);
		x1 = leftRet.get();
		x2 = (rightRet.get() == -1) ? data.w-1 : data.w-1-rightRet.get();
		
		if( topRet.get() == -1)
			return new Rect(0,0,0,0);
		y1 = topRet.get();
		y2 = (bottomRet.get() == -1) ? data.h-1 : data.h-1-bottomRet.get();

		x1 = Math.max(0, x1 - buffer);
		y1 = Math.max(0, y1 - buffer);
		x2 = Math.min(data.w-1, x2 + buffer);
		y2 = Math.min(data.h-1, y2 + buffer);
		
		if( raw.isGLOriented()) {
			int t = y1;
			y1 = data.h-y2;
			y2 = data.h-t;
		}
		
		// TODO: Figure out why the -1 in y1-1 is needed.  It definitely shouldn't be.
		return new Rect( x1, y1-1, x2-x1+1, y2-y1+1 );
	}
	private static abstract class _ImageCropHelper {
		final int w;
		final int h;
		
		_ImageCropHelper( BufferedImage image ) {
			this.w = image.getWidth();
			this.h = image.getHeight();
		}
		_ImageCropHelper( int w, int h) {
			this.w = w;
			this.h = h;
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
		_ImageCropHelperInt( int[] data, int w, int h) {
			super(w, h);
			this.pixels = data;
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




}
