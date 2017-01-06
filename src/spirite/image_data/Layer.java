package spirite.image_data;

import java.awt.Color;
import java.util.List;

import spirite.image_data.ImageWorkspace.StructureChangeEvent.ChangeType;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;


// !!!! For now I'm going with a 1 Rig : 1 Part for simplicity
public class Layer {
	// This feels bad but I don't like the idea of having 1000 nested classes 
	//	in a monstrous, unnavigable ImageWorkspace.java file
	ImageWorkspace context;	
//	private List<Layer> subrigs;
//	private List<Part> parts;
//	private Part debug_part;
//	int cx, cy;		// Translation X/Y
//	float rx;		// Rotation X/Y
//	float sx, sy;	// Scale X/Y
	String name = "";
	
	Layer( ImageWorkspace context) {
		this.context = context;
	}
	
/*	public Layer( int x, int y, String name, Color c) {
		debug_part = new Part(x, y, name, c);
		this.name = name;
	}*/
	
	public ImageData getActiveData() {
		return null;
	}
	
	public String getName() {
		return name;
	}
	public void setName( String name) {
		this.name = name;
	}
}
