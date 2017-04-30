package spirite.pc.graphics;

import java.awt.image.BufferedImage;

import spirite.base.image_data.RawImage;
import spirite.base.util.MUtil;

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
		return new ImageBI( MUtil.deepCopy(img));
	}

}
