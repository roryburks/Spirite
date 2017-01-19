package spirite.image_data.layers;

import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;

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
		g.drawImage(data.readImage().image, 0, 0, null);
		
	}

	@Override
	public ImageData getActiveData() {
		return data;
	}

	@Override
	public int getWidth() {
		return data.readImage().getWidth();
	}

	@Override
	public int getHeight() {
		return data.readImage().getHeight();
	}

}
