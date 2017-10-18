package spirite.hybrid;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.swing.JOptionPane;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.RawImage;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.GLImage;
import spirite.base.graphics.gl.wrap.GLCore;
import spirite.base.graphics.gl.wrap.GLCore.MGLException;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;
import spirite.pc.jogl.JOGLCore;
import spirite.pc.jogl.JOGLCore.OnGLLoadObserver;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;


/** 
 * 
 * 
 * @author Rory Burks
 *
 */
public class HybridHelper {
//	public class HybridHelper {
//		public static GLCore getGLCore() {return null;}
//		public static boolean showConfirm( String title, String message) {return false;}
//		public static void showMessage( String title, String message) {}
//		public static RawImage createImage( int width, int height) {}
//		public static Class<? extends RawImage> getImageType() {}
//		
//		public static void imageToClipboard( RawImage image) {}
//		public static RawImage imageFromClipboard() {}
//		public static void loadImageIntoGL(RawImage image, GL2 gl) {}
//	}

	public static int BI_FORMAT = BufferedImage.TYPE_INT_ARGB;
	
	private static boolean useGL = true;
	public static boolean isUsingGL() {return useGL;}

	private static GLCore core;
	public static GLCore getGLCore() {
		if( core == null) core = new JOGLCore();
		return core;
	}

    private static boolean initGL() {
    	try {
    		GLEngine engine = GLEngine.getInstance();
    		JOGLCore.init(new OnGLLoadObserver() {
				@Override
				public void onLoad(GL2 gl) throws MGLException {
					engine.init(gl);
				}
			});
    	}catch( Exception e) { 
    		MDebug.handleError(ErrorType.ALLOCATION_FAILED, "Could not create OpenGL Context: \n" + e.getMessage());
    		return false;
    	}
    	return true;
    }
	
	// ============
	// ==== Mesages
	public static boolean showConfirm( String title, String message) {
		int r = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
		
		return r == JOptionPane.YES_OPTION;
			
	}
	public static void showMessage( String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.OK_OPTION);
	}

	public static RawImage createImageNonNillable( int width, int height) throws InvalidImageDimensionsExeption {
		if( width <= 0 || height <= 0)
			throw new InvalidImageDimensionsExeption("Bad Image Dimensions");
		if( useGL) {
			initGL();
			return new GLImage(width,height);
		}
		else
			return new ImageBI(new BufferedImage(width, height, BI_FORMAT));
	}
	public static RawImage createImage( int width, int height) {
		try {
			return createImageNonNillable(width,height);
		} catch (InvalidImageDimensionsExeption e) {
			MDebug.handleError(ErrorType.STRUCTURAL_MAJOR, e, "Couldn't create 1x1 image");
			return null;
		}
	}
	public static RawImage createNillImage() {
		try {
			return createImageNonNillable(1,1);
		} catch (InvalidImageDimensionsExeption e) {
			MDebug.handleError(ErrorType.STRUCTURAL_MAJOR, e, "Couldn't create 1x1 image");
			return null;
		}
	}
	
	public static Class<? extends RawImage> getImageType() {
		return (useGL)?GLImage.class:ImageBI.class;
	}
	
	// ===========
	// ==== Clipboard
	public static void imageToClipboard( RawImage image) {
    	TransferableImage transfer = new TransferableImage(
    			((ImageBI)HybridUtil.convert(image, ImageBI.class)).img);
    	
    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    	c.setContents(transfer, null);
	}


	/** Attempts to get an image from the clipboard, returning null if 
	 * there is no image or for some reason you can't get it. 
	 * @return
	 */
	public static RawImage imageFromClipboard() {
		try {
	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	
			// Convert Clipboard image into BufferedImage
			Image img = (Image)c.getData(DataFlavor.imageFlavor);
			
			
    		BufferedImage bi = new BufferedImage(  
    				img.getWidth(null), img.getHeight(null), HybridHelper.BI_FORMAT);
    		Graphics g = bi.getGraphics();
    		g.drawImage(img, 0, 0, null);
    		g.dispose();

    		return new ImageBI(bi);
		} catch( IOException | UnsupportedFlavorException e) {}
		
		return null;
	}

	// =========
	// === GL
	public static void loadImageIntoGL(RawImage image, GL2 gl) {
		if( image instanceof ImageBI) {
			_loadBI( ((ImageBI) image).img, gl);
		}
		else if( image instanceof GLImage) {
			GLImage gli = (GLImage)image;
			GLEngine engine = GLEngine.getInstance();
			engine.setTarget(gli);
			gl.glCopyTexImage2D(
					GL2.GL_TEXTURE_2D,
					0,
					GL2.GL_RGBA,
					0, 0, gli.getWidth(), gli.getHeight(),
					0);
			engine.setTarget(0);
		}
		else {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported IntoGL");
		}
	}
	private static void _loadBI( BufferedImage bi, GL2 gl) {
		WritableRaster rast = bi.getRaster();
		int w = bi.getWidth();
		int h = bi.getHeight();

		if( rast instanceof ByteInterleavedRaster) {
			gl.glTexImage2D(
					GL2.GL_TEXTURE_2D,
					0,
					GL2.GL_RGBA,
					w, h,
					0,
					GL2.GL_RGBA,
					GL2.GL_UNSIGNED_INT_8_8_8_8,
					ByteBuffer.wrap(((ByteInterleavedRaster)rast).getDataStorage())
					);
		}
		if( rast instanceof IntegerInterleavedRaster) {
			gl.glTexImage2D(
					GL2.GL_TEXTURE_2D,
					0,
					GL2.GL_RGBA,
					w, h,
					0,
					GL2.GL_BGRA,
					GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
					IntBuffer.wrap(((IntegerInterleavedRaster)rast).getDataStorage())
					);
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


	public static void beep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
		
	}
}
