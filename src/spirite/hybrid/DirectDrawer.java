package spirite.hybrid;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.GLImage;
import spirite.base.image_data.RawImage;
import spirite.base.util.DataCompaction.IntQueue;
import spirite.base.util.MUtil;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;
import spirite.pc.jogl.JOGLCore;
import sun.awt.image.ByteInterleavedRaster;

public class DirectDrawer {
	public static void fill( RawImage img, int x, int y, int color) {
		if( img instanceof ImageBI) {
			fillBI( ((ImageBI) img).img, x, y, color);
		}
		else if( img instanceof GLImage) {
			fillGI( (GLImage) img, x, y, color);
		}
		else {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported Fill Image Type.");
		}
	}
	
	private static void fillGI( GLImage img, int x, int y, int c) {
		GLEngine engine = GLEngine.getInstance();
		GL2 gl = engine.getGL2();
		int w = img.getWidth();
		int h = img.getHeight();
		y = h-y;
		
		engine.setTarget(img);

		IntBuffer read = IntBuffer.allocate(w*h);
		int buffer[] = read.array();
		gl.glReadnPixels( 0, 0, w, h, 
				GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, 4*w*h, read);

		int bg = read.get(y*w+x);
		
		IntQueue queue = new IntQueue();
		queue.add( MUtil.packInt(x, y));
		if( bg == c) return;
		
		
		while( !queue.isEmpty()) {
			int p = queue.poll();
			int ix = MUtil.high16(p);
			int iy = MUtil.low16(p);
			
			if( read.get(iy*w+ix) != bg)
				continue;
				
			read.put(iy*w+ix, c);

			if( ix + 1 < w) {
				queue.add( MUtil.packInt(ix+1, iy));
			}
			if( ix - 1 >= 0) {
				queue.add( MUtil.packInt(ix-1, iy));
			}
			if( iy + 1 < h) {
				queue.add( MUtil.packInt(ix, iy+1));
			}
			if( iy - 1 >= 0) {
				queue.add( MUtil.packInt(ix, iy-1));
			}
		}

		gl.glBindTexture(GL2.GL_TEXTURE_2D, img.getTexID());
		gl.glTexImage2D(
				GL2.GL_TEXTURE_2D,
				0,
				GL2.GL_RGBA,
				w, h,
				0,
				GL2.GL_BGRA,
				GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
				read
				);

	}
	
	private static void fillBI( BufferedImage bi, int x, int y, int c) {
		int w = bi.getWidth();
		int h = bi.getHeight();
		int bg = bi.getRGB(x, y);

		IntQueue queue = new IntQueue();
		
		queue.add( MUtil.packInt(x, y));
		if( bg == c) return;
		
		while( !queue.isEmpty()) {
			int p = queue.poll();
			int ix = MUtil.high16(p);
			int iy = MUtil.low16(p);
			
			if( bi.getRGB(ix, iy) != bg)
				continue;
				
			bi.setRGB(ix, iy, c);

			if( ix + 1 < w) {
				queue.add( MUtil.packInt(ix+1, iy));
			}
			if( ix - 1 >= 0) {
				queue.add( MUtil.packInt(ix-1, iy));
			}
			if( iy + 1 < h) {
				queue.add( MUtil.packInt(ix, iy+1));
			}
			if( iy - 1 >= 0) {
				queue.add( MUtil.packInt(ix, iy-1));
			}
		}
	}
}
