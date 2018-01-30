package sjunit;

import org.junit.Test;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.util.linear.Rect;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;

public class BasicImageTest {
	final static boolean deep = false;

	@Test
	public void fill() throws Exception {
		TestWrapper.performTest((MasterControl) -> {
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
		});
	}
	
	@Test
	public void testContentBounds() throws Exception {

		TestWrapper.performTest((MasterControl) -> {
			RawImage img = HybridHelper.createImage(10, 10);
			
	
			GraphicsContext gc = img.getGraphics();
			gc.clear();
			gc.fillRect(2, 3, 4, 3);
			
			Rect r = HybridUtil.findContentBounds(img, 0, true);
			
			assert( r.getX() == 2 && r.getY() == 3 && r.getWidth() == 4 && r.getHeight() == 3);
			
	
			RawImage nri = HybridHelper.createImage(r.getWidth(), r.getHeight());
			gc = nri.getGraphics();
			gc.clear();
			gc.drawImage( img, -r.getX(), -r.getY());
			
			r = HybridUtil.findContentBounds(nri, 0, true);
			
	
			//System.out.println( r.getX() + "," + r.getY() + ":" + r.getWidth() + "," + r.getHeight());
			assert( r.getX() == 0 && r.getY() == 0 && r.getWidth() == 4 && r.getHeight() == 3);
			
	
			RawImage img3 = HybridHelper.createImage(10, 10);
			gc = img3.getGraphics();
			gc.clear();
			gc.fillRect(5, 5, 5, 5);
			r = HybridUtil.findContentBounds(img3, 0, true);
			assert( r.getX() == 5 && r.getY() == 5 && r.getWidth() == 5 && r.getHeight() == 5);
		});
	}

	@Test
	public void deepCopy() throws Exception {

		TestWrapper.performTest((MasterControl) -> {
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
		});
	}
	
	@Test
	public void t() throws Exception {
		TestWrapper.performTest((MasterControl) -> {
			RawImage img = HybridHelper.createImage(10, 10);
		});
		
	}
}
