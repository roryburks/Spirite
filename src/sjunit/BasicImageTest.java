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
import spirite.base.util.glmath.Rect;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
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

		gc.setColor(0xFFFF0000);
		gc.fillRect(0, 0, 300, 300);

		for( int x=0; x<299; x += deep ? 1 : 10) {
			for( int y=0; y<299; y += deep ? 1 : 10) {
				assert( img.getRGB(x, y) == 0xFFFF0000);
			}
		}
	}
	
	@Test
	public void testContentBounds() throws UnsupportedImageTypeException {
		RawImage img = HybridHelper.createImage(10, 10);
		

		GraphicsContext gc = img.getGraphics();
		gc.clear();
		gc.fillRect(2, 3, 4, 3);
		
		Rect r = HybridUtil.findContentBounds(img, 0, true);
		
		assert( r.x == 2 && r.y == 3 && r.width == 4 && r.height == 3);
		

		RawImage nri = HybridHelper.createImage(r.width, r.height);
		gc = nri.getGraphics();
		gc.clear();
		gc.drawImage( img, -r.x, -r.y);
		
		r = HybridUtil.findContentBounds(nri, 0, true);
		

		//System.out.println( r.x + "," + r.y + ":" + r.width + "," + r.height);
		assert( r.x == 0 && r.y == 0 && r.width == 4 && r.height == 3);
		

		RawImage img3 = HybridHelper.createImage(10, 10);
		gc = img3.getGraphics();
		gc.clear();
		gc.fillRect(5, 5, 5, 5);
		r = HybridUtil.findContentBounds(img3, 0, true);
		assert( r.x == 5 && r.y == 5 && r.width == 5 && r.height == 5);
	}

	@Test
	public void deepCopy() {
		RawImage img1 = HybridHelper.createImage(10, 10);
		RawImage img3 = HybridHelper.createImage(10, 10);

		GraphicsContext gc1 = img1.getGraphics();
		gc1.clear();
		gc1.setColor(0xFFFF0000);
		gc1.fillRect(0, 0, 5, 5);

		// Set a new image context to try to confuse the engine
		GraphicsContext gc3 = img3.getGraphics();
		gc3.clear();
		gc3.setColor(0xFFF0F0F0);
		gc3.fillRect(0, 0, 5, 5);
		
		// Verify that the image is a duplicate
		RawImage img2 = img1.deepCopy();

		for( int x=0; x<10; ++x) {
			for( int y=0; y<10; ++y) {
				assert( img2.getRGB(x, y) == img1.getRGB(x, y));
			}
		}
		
		// Verify that changing Img1 doesn't affect Img2
		gc1.setColor(0xFF00FF00);
		gc1.fillRect(0, 0, 5, 5);
		for( int x=0; x<4; ++x) {
			for( int y=0; y<4; ++y) {
				assert( img1.getRGB(x, y) == 0xFF00FF00);
				assert( img2.getRGB(x, y) == 0xFFFF0000);
			}
		}
		
		// Verify that changing Img2 doesn't affect Img1
		GraphicsContext gc2 = img2.getGraphics();
		gc2.setColor(0xFF0000FF);
		gc2.fillRect(0, 0, 5, 5);
		gc2.fillRect(0, 0, 5, 5);
		for( int x=0; x<4; ++x) {
			for( int y=0; y<4; ++y) {
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
