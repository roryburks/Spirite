package spirite.hybrid;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.UnsupportedDataTypeException;
import javax.imageio.ImageIO;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.RawImage;
import spirite.base.util.MUtil;
import spirite.base.util.ArrayInterpretation.IntCounter;
import spirite.base.util.ArrayInterpretation.InterpretedIntArray;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;

public class HybridUtil {
	public static class UnsupportedImageTypeException extends Exception {
		private UnsupportedImageTypeException(String message) {super(message);}
	}
	
	
	// =======
	// ==== IO
	public static RawImage load( File f) throws IOException {
		BufferedImage bi = ImageIO.read(f);
		RawImage img = HybridHelper.createImage(bi.getWidth(), bi.getHeight());
		GraphicsContext gc = img.getGraphics();
		gc.clear();
		gc.drawImage( new ImageBI(bi), 0, 0);
//		g.dispose();
		
		return img;
	}

	public static RawImage load(ByteArrayInputStream byteArrayInputStream) throws IOException {
		BufferedImage bi = ImageIO.read(byteArrayInputStream);
		RawImage img = HybridHelper.createImage(bi.getWidth(), bi.getHeight());
		GraphicsContext gc = img.getGraphics();
		gc.clear();
		gc.drawImage( new ImageBI(bi), 0, 0);
//		g.dispose();
		
		return img;
	}
	
	public static void savePNG( RawImage raw, OutputStream os) throws IOException {
		if( raw instanceof ImageBI) {
			ImageIO.write( ((ImageBI) raw).img, "png", os);
		}
		else {
			MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unsupported Image Type in savePNG");
		}
	}

	public static void savePNG(RawImage raw, String ext, File f) throws IOException {
		if( raw instanceof ImageBI) {
			ImageIO.write( ((ImageBI) raw).img, ext, f);
		}
		else {
			MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Unsupported Image Type in savePNG");
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
	public static Rect findContentBounds( RawImage raw, int buffer, boolean transparentOnly) 
			throws UnsupportedImageTypeException 
	{
		if( !(raw instanceof ImageBI))
			throw new UnsupportedImageTypeException("Unsupported RawImage type");
		BufferedImage image = ((ImageBI)raw).img;

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
			throw new UnsupportedImageTypeException("Only programmed to deal with 4-byte color data.");
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




}
