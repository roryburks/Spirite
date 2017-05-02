package spirite.pc;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.gl.engine.GLEngine;
import spirite.base.graphics.gl.engine.GLImage;
import spirite.hybrid.HybridHelper;
import spirite.pc.jogl.JOGLCore;
import sun.awt.image.IntegerInterleavedRaster;

public class PCUtil {
	public static BufferedImage deepCopy( BufferedImage toCopy) {
		return new BufferedImage( 
				toCopy.getColorModel(),
				toCopy.copyData(null),
				toCopy.isAlphaPremultiplied(),
				null);
	}
	
	public static BufferedImage glToBI( GLImage img) {
		GLEngine engine = GLEngine.getInstance();
		engine.setTarget(img);
		BufferedImage bi = glSurfaceToImage( HybridHelper.BI_FORMAT, img.getWidth(), img.getHeight());
		engine.setTarget(0);
		return bi;
	}
	
	/** Writes the active GL Surface to a BufferedImage */
	public static BufferedImage glSurfaceToImage(int type, int width, int height) {
		GL2 gl = JOGLCore.getGL2();
		BufferedImage bi = null;
//		long time;
		
//		time = System.currentTimeMillis();
		
		switch( type) {
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE:
		bi = new BufferedImage(width, height, type);
		
		IntegerInterleavedRaster iir = (IntegerInterleavedRaster)bi.getRaster();
		IntBuffer ib = IntBuffer.wrap(iir.getDataStorage());
		
		gl.glReadPixels( 0, 0, width, height, 
				GL2.GL_BGRA,
				GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
				ib);
		break;
		default: 
			return null;
		}
		
		// Flip Vertically
		if( true) {
			final WritableRaster raster = bi.getRaster();
			Object scanline1 = null;
			Object scanline2 = null;
			for( int i=0; i<bi.getHeight()/2; ++i) {
				scanline1 = raster.getDataElements(0, i, bi.getWidth(), 1, scanline1);
				scanline2 = raster.getDataElements(0, bi.getHeight()-i-1, bi.getWidth(), 1, scanline2);
				raster.setDataElements(0, i, bi.getWidth(), 1, scanline2);
				raster.setDataElements(0, bi.getHeight()-i-1, bi.getWidth(), 1, scanline1);
			}
		}
//		time = System.currentTimeMillis();
//		bi = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
//        		.readPixelsToBufferedImage( getGL2(), 0, 0, width, height, true);
//        System.out.println("AWT: " + (System.currentTimeMillis()-time));
        
		return bi;
		
	}
}
