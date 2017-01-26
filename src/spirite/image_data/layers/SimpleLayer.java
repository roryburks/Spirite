package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.image_data.ImageHandle;

public class SimpleLayer extends Layer {
	private final ImageHandle data;
	
	public SimpleLayer( ImageHandle data) {
		this.data = data;
	}
	
	public ImageHandle getData() {
		return data;
	}

	@Override
	public List<ImageHandle> getUsedImageData() {
		return Arrays.asList(data);
	}

	@Override
	public void draw(Graphics g) {
		data.drawLayer(g);
	}

	@Override
	public ImageHandle getActiveData() {
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
	public Layer logicalDuplicate() {
		return new SimpleLayer(data.dupe());
	}

	@Override
	public List<Rectangle> interpretCrop(Rectangle rect) {
		Rectangle bounds = new Rectangle(data.getWidth(), data.getHeight());
		
		List<Rectangle> list = new ArrayList<>(1);
		list.add( bounds.intersection(rect));
		
		return list;
	}

}
