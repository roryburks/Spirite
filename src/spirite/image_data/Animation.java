package spirite.image_data;

import java.util.List;

import spirite.graphics.GraphicsContext;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;

public abstract class Animation {
	protected String name;
	AnimationManager context;
	
	protected void triggerChange() {if( context != null) context.triggerChangeAnimation(this);}
	public abstract void drawFrame( GraphicsContext gc, float t);
	public abstract float getStartFrame();
	public abstract float getEndFrame();
	public abstract void importGroup( GroupNode node);

	public abstract List<GroupNode> getGroupLinks();
	public abstract void interpretChange( GroupNode node, StructureChangeEvent evt);
	
	public String getName() {
		return name;
	}
	public void setName( String name) {
		this.name = name;
	}
}
