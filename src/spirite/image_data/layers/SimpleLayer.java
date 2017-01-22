package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import spirite.MUtil;
import spirite.image_data.ImageData;

public class SimpleLayer extends Layer {
	private final ImageData data;
	
	public SimpleLayer( ImageData data) {
		this.data = data;
	}
	
	public ImageData getData() {
		return data;
	}

	@Override
	public List<ImageData> getUsedImageData() {
		return Arrays.asList(data);
	}

	@Override
	public void draw(Graphics g) {
		data.drawLayer(g);
	}

	@Override
	public ImageData getActiveData() {
		return data;
	}

	@Override
	public int getWidth() {
		return data.getWidth();
	}

	@Override
	public int getHeight() {
		return data.getHeight();
	}

	@Override
	public Layer duplicate() {
		BufferedImage bi = MUtil.deepCopy(data.deepAccess());
		ImageData dupe = new ImageData(bi, -1, data.getContext());
		
		return new SimpleLayer(dupe);
	}

}
