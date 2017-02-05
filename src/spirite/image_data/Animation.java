package spirite.image_data;

import java.awt.Graphics;

import spirite.image_data.GroupTree.GroupNode;

public abstract class Animation {
	protected String name;
	AnimationManager context;
	
	protected void triggerChange() {if( context != null) context.triggerChangeAnimation(this);}
	public abstract void drawFrame( Graphics g, float t);
	public abstract float getStartFrame();
	public abstract float getEndFrame();
	public abstract void interpretLink( GroupNode node);
	public abstract void importGroup( GroupNode node);
	
	public String getName() {
		return name;
	}
	public void setName( String name) {
		this.name = name;
	}
}
