package spirite.hybrid;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.swing.JOptionPane;

import com.jogamp.opengl.GL2;

import spirite.base.image_data.RawImage;
import spirite.base.util.MUtil.TransferableImage;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

public class HybridHelper {

	public static int BI_FORMAT = BufferedImage.TYPE_INT_ARGB_PRE;
	
	public static boolean showConfirm( String title, String message) {
		int r = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
		
		return r == JOptionPane.YES_OPTION;
			
	}

	public static RawImage createImage( int width, int height) {
		return new ImageBI(new BufferedImage(width, height, BI_FORMAT));
	}
	
	public static void imageToClipboard( RawImage image) {
		if( image instanceof ImageBI) {
	    	TransferableImage transfer = new TransferableImage(((ImageBI) image).img);
	    	
	    	Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    	c.setContents(transfer, null);
		}
		else {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported Image");
		}
	}

	public static void loadImageIntoGL(RawImage image, GL2 gl) {
		if( image instanceof ImageBI) {
			_loadBI( ((ImageBI) image).img, gl);
		}
		else {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported Image");
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
}
