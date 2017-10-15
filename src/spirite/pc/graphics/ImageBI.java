package spirite.pc.graphics;

import java.awt.image.BufferedImage;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.pc.PCUtil;
import spirite.pc.graphics.awt.AWTContext;

public class ImageBI extends RawImage {
	public final BufferedImage img;

	public ImageBI( BufferedImage bi) {
		this.img = bi;
	}
	

	@Override
	public int getWidth() {
		return img.getWidth();
	}

	@Override
	public int getHeight() {
		return img.getHeight();
	}

	@Override
	public void flush() {
		img.flush();
	}

	@Override
	public int getByteSize() {
		return (img.getWidth()*img.getHeight()*img.getColorModel().getPixelSize() + 7)/8;
	}


	@Override
	public RawImage deepCopy() {
		return new ImageBI( PCUtil.deepCopy(img));
	}


	@Override
	public GraphicsContext getGraphics() {
		return new AWTContext( img.getGraphics(), img.getWidth(), img.getHeight());
	}


	@Override
	public int getRGB(int x, int y) {
		return img.getRGB(x, y);
	}


	@Override
	public boolean isGLOriented() {
		return false;
	}

}
