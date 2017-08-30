package sjunit;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.Test;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.gl.GLCache;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.wrap.GLCore.MGLException;
import spirite.base.image_data.RawImage;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.pc.jogl.JOGLCore;
import spirite.pc.jogl.JOGLCore.OnGLLoadObserver;

public class BasicImageTest {
	final static boolean deep = false;

	@Test
	public void fill() {
		RawImage img = HybridHelper.createImage(500, 400);
		
		assert(img.getWidth() == 500);
		assert(img.getHeight() == 400);
		

		GraphicsContext gc = img.getGraphics();
		gc.clear();

		for( int x=0; x<img.getWidth(); x += deep ? 1 : 10) {
			for( int y=0; y<img.getHeight(); y += deep ? 1 : 10) {
				assert( img.getRGB(x, y) == 0x00000000);
			}
		}

	    // TODO: Known problem: GLGraphics' fillRect has known off-by-one problems
		gc.setColor(0xFFFF0000);
		gc.fillRect(0, 0, 300, 300);

		for( int x=1; x<299; x += deep ? 1 : 10) {
			for( int y=1; y<299; y += deep ? 1 : 10) {
				assert( img.getRGB(x, y) == 0xFFFF0000);
			}
		}
	}

	@Test
	public void deepCopy() {
		RawImage img1 = HybridHelper.createImage(10, 10);

		GraphicsContext gc1 = img1.getGraphics();
		gc1.clear();
		gc1.setColor(0xFFFF0000);
		gc1.fillRect(0, 0, 5, 5);
		
		// Verify that the image is a duplicate
		RawImage img2 = img1.deepCopy();

		for( int x=1; x<4; ++x) {
			for( int y=1; y<4; ++y) {
				assert( img2.getRGB(x, y) == 0xFFFF0000);
			}
		}
		
		// Verify that changing Img1 doesn't affect Img2
		gc1.setColor(0xFF00FF00);
		gc1.fillRect(0, 0, 5, 5);
		for( int x=1; x<4; ++x) {
			for( int y=1; y<4; ++y) {
				assert( img1.getRGB(x, y) == 0xFF00FF00);
				assert( img2.getRGB(x, y) == 0xFFFF0000);
			}
		}
		
		// Verify that changing Img2 doesn't affect Img1
		GraphicsContext gc2 = img2.getGraphics();
		gc2.setColor(0xFF0000FF);
		gc2.fillRect(0, 0, 5, 5);
		gc2.fillRect(0, 0, 5, 5);
		for( int x=1; x<4; ++x) {
			for( int y=1; y<4; ++y) {
				assert( img1.getRGB(x, y) == 0xFF00FF00);
				assert( img2.getRGB(x, y) == 0xFF0000FF);
			}
		}
	}
	
	@Test
	public void t() {
		RawImage img = HybridHelper.createImage(10, 10);
		
	}
}
