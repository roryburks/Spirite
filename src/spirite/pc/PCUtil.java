package spirite.pc;

import com.jogamp.opengl.GL2;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.GLImage;
import spirite.base.util.linear.MutableTransform;
import spirite.base.util.linear.Transform;
import spirite.hybrid.HybridHelper;
import spirite.pc.jogl.JOGLCore;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;

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
        
		return bi;
		
	}
	
	/** Converts a MatTrans to an AffineTransform */
	public static AffineTransform toAT( Transform trans) {
		return new AffineTransform(
				trans.getM00(), trans.getM10(), trans.getM01(),
				trans.getM11(), trans.getM02(), trans.getM12());
	}
	/** Converts an AffineTransform to a MatTrans */
	public static MutableTransform toMT(AffineTransform trans) {
		return new MutableTransform(
				(float)trans.getScaleX(), (float)trans.getShearX(), (float)trans.getTranslateX(),
				(float)trans.getShearY(), (float)trans.getScaleY(), (float)trans.getTranslateY());
	}
}
