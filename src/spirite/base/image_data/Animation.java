package spirite.base.image_data;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.Node;

/**
 * An animation essentially performs one thing: given a timeframe t, it draws
 * a certain thing.  In general it relies on Mediums (which must be managed and 
 * handled separately by being within a node in addition to being in an Animation), 
 * but it's perfectly possible to construct an entirely abstract Animation.
 */
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
	public abstract void drawFrame(GraphicsContext gc, float t);
	public abstract List<TransformedHandle> getDrawList( float t);
	public abstract List<List<TransformedHandle>> getDrawTable( float t, AnimationState state);
	public abstract float getStartFrame();
	public abstract float getEndFrame();
	public abstract boolean isFixedFrame();
	public abstract void purge();

	public abstract List<Node> getNodeLinks();
	public abstract void nodesChanged( List<Node> nodes);
	
	public String getName() {
		return name;
	}
	public void setName( String name) {
		this.name = name;
	}
}
