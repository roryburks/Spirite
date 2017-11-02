package spirite.base.graphics.renderer.sources;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ReferenceManager.Reference;
import spirite.hybrid.HybridHelper;

/** This renders the Reference section, either the front section (the part placed
 * over the image) or the back section (the part placed behind). */
public class ReferenceRenderSource extends RenderSource {

	private final boolean front;
	public ReferenceRenderSource( ImageWorkspace workspace, boolean front) {
		super(workspace);
		this.front = front;
	}
	
	@Override
	public int getDefaultWidth() { return workspace.getWidth(); }
	@Override
	public int getDefaultHeight() { return workspace.getHeight(); }
	@Override
	public List<MediumHandle> getImagesReliedOn() {
		return workspace.getReferenceManager().getDependencies(front);
	}

	@Override
	public RawImage render(RenderSettings settings) {
		try {
			RawImage img = HybridHelper.createImageNonNillable(settings.width, settings.height);
			
			GraphicsContext gc = img.getGraphics();
			
			List<Reference> refList = workspace.getReferenceManager().getList(front);
			float rw = settings.width / (float)workspace.getWidth();
			float rh = settings.height / (float)workspace.getHeight();
			gc.scale( rw, rh);
					
			for( Reference ref : refList ) {
				ref.draw(gc);
			}
			
	//		gc.dispose();
			return img;
    	} catch(InvalidImageDimensionsExeption e) {
    		return HybridHelper.createNillImage();
    	}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (front ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReferenceRenderSource other = (ReferenceRenderSource) obj;
		if (front != other.front)
			return false;
		return true;
	}
}