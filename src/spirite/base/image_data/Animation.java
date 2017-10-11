package spirite.base.image_data;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;

public abstract class Animation {
	protected String name;
	AnimationManager context;
	
	protected void triggerChange() {if( context != null) context.triggerChangeAnimation(this);}
	public abstract void drawFrame( GraphicsContext gc, float t);
	public abstract List<TransformedHandle> getDrawList( float t);
	public abstract List<List<TransformedHandle>> getDrawTable( float t, AnimationState state);
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
