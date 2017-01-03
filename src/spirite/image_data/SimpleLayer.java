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
	Part part;

	public SimpleLayer(int x, int y, String name, Color c) {
		// TODO Auto-generated constructor stub
		this.name = name;
		
		part = new Part(x, y, "simple", c);
	}

	@Override
	public Part getActivePart() {
		return part;
	}
}
