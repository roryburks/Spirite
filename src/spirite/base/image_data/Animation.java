package spirite.base.image_data;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.GroupNode;

public abstract class Animation {
	protected String name;
	protected ImageWorkspace context;
	protected AnimationManager animationManager;
	
	protected Animation( ImageWorkspace context) {
		this.context = context;
		this.animationManager = context.getAnimationManager();
	}
	
	void setContext(ImageWorkspace context) {
		this.context = context;
		this.animationManager = context.getAnimationManager();
	}
	
	
	protected void triggerChange() {if( animationManager != null) animationManager.triggerChangeAnimation(this);}
	public abstract void drawFrame( GraphicsContext gc, float t);
	public abstract List<TransformedHandle> getDrawList( float t);
	public abstract List<List<TransformedHandle>> getDrawTable( float t, AnimationState state);
	public abstract float getStartFrame();
	public abstract float getEndFrame();
	public abstract void importGroup( GroupNode node);
	public abstract boolean isFixedFrame();
	public abstract void purge();

	public abstract List<GroupNode> getGroupLinks();
	public abstract void groupChanged( GroupNode node);
	
	public String getName() {
		return name;
	}
	public void setName( String name) {
		this.name = name;
	}
}
