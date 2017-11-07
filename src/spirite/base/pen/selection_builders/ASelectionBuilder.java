package spirite.base.pen.selection_builders;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;

/**
 * 
 *
 * NOTE: Could be worth considering moving ImageWorkspace being passed from the 
 * constructor to build, which is the only thing that uses it.  Or even making
 * build( width, height) instead
 */
public abstract class ASelectionBuilder {
	protected final ImageWorkspace context;
	protected ASelectionBuilder( ImageWorkspace ws) {
		this.context = ws;
	}
	
	public abstract void start( int x, int y);
	public abstract void update( int x, int y);
	public abstract RawImage build();
	public abstract void draw( GraphicsContext gc);
}