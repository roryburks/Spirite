package spirite.image_data;

import java.awt.Color;
import java.util.List;


// !!!! For now I'm going with a 1 Rig : 1 Part for simplicity
public class Layer {
//	private List<Layer> subrigs;
//	private List<Part> parts;
//	private Part debug_part;
//	int cx, cy;		// Translation X/Y
//	float rx;		// Rotation X/Y
//	float sx, sy;	// Scale X/Y
	String name = "";
	
/*	public Layer( int x, int y, String name, Color c) {
		debug_part = new Part(x, y, name, c);
		this.name = name;
	}*/
	
	public Part getActivePart() {
		return null;
	}
	
	public String getName() {
		return name;
	}
}
