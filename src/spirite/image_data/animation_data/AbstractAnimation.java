package spirite.image_data.animation_data;

import java.awt.Graphics;

import spirite.image_data.GroupTree.GroupNode;

public abstract class AbstractAnimation {
	String name;
	
	public abstract void drawFrame( Graphics g, float t);
	public abstract float getStartFrame();
	public abstract float getEndFrame();
	public abstract void interpretLink( GroupNode node);
	
	public String getName() {
		return name;
	}
}
