package spirite.base.graphics.renderer.sources;

import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.MediumHandle;

import java.util.ArrayList;
import java.util.List;

/**
 * A RenderSource corresponds to an object which can be rendered and it implements
 * everything needed to perform a Render using certain RenderSettings.
 *
 * Note: It is important that subclasses overload the equals and hashCode methods
 * of each RenderSource since the RenderEngine uses them to determine if you
 * are rendering the same thing as something that has already been rendered.
 * If you just go on the built-in uniqueness test and pass them through renderImage
 * then unless you are storing the RenderSource locally yourself (which is possible
 * and not harmful but defeats the purpose of RenderEngine), then RenderEngine 
 * will get clogged remembering different renders of the same image.
 */
public abstract class RenderSource {
	public final ImageWorkspace workspace;
	RenderSource( ImageWorkspace workspace) {this.workspace = workspace;}
	public abstract int getDefaultWidth();
	public abstract int getDefaultHeight();
	public abstract List<MediumHandle> getImagesReliedOn();
	public List<Node> getNodesReliedOn() {return new ArrayList<>(0);}
	public abstract RawImage render( RenderSettings settings);
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RenderSource other = (RenderSource) obj;
		if (workspace == null) {
			if (other.workspace != null)
				return false;
		} else if (!workspace.equals(other.workspace))
			return false;
		return true;
	}
}