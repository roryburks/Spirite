package sjunit;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.Test;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.gl.GLImage;

public class ImageTest {

	@Test
	public void test() throws Exception {
		TestWrapper.performTest((MasterControl master) -> {
			GLImage image;
			try {
				image = new GLImage(400,500);
				
				assert( image.getWidth() == 400);
				assert( image.getHeight() == 500);
				
				
				GraphicsContext gc = image.getGraphics();
				gc.setColor( 0xFFFF0000);
				gc.fillRect(0, 0, 50, 50);
				
				assert(image.getRGB(25, 25)== 0xFFFF0000);
			} catch (InvalidImageDimensionsExeption e) {
				assert(false);
			}
		});
	}

}
