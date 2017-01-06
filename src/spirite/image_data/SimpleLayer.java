package spirite.image_data;

import java.awt.Color;

/**
 * A SimpleLayer has only one part of data.
 * 
 * @author Rory Burks
 *
 */
public class SimpleLayer extends Layer
{
	ImageData part;

	public SimpleLayer( ImageData data, String name, ImageWorkspace context) {
		super(context);
		part = data;
		this.name = name;
	}
	
	public SimpleLayer(int x, int y, String name, Color c, ImageWorkspace context) {
		super(context);
		// TODO Auto-generated constructor stub
		this.name = name;
		
		part = new ImageData(x, y, c);
	}

	@Override
	public ImageData getActiveData() {
		return part;
	}
}
