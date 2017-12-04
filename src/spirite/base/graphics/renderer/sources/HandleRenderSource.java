package spirite.base.graphics.renderer.sources;

import java.util.Arrays;
import java.util.List;

import spirite.base.graphics.RawImage;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.MediumHandle;
import spirite.hybrid.HybridHelper;

/** This renders an Image rather plainly. */
public class HandleRenderSource extends RenderSource {
	private final MediumHandle handle;
	public HandleRenderSource( MediumHandle handle) {
		super(handle.getContext());
		this.handle = handle;
	}
	
	@Override public int getDefaultWidth() { return handle.getWidth(); }
	@Override public int getDefaultHeight() { return handle.getHeight(); }
	@Override public List<MediumHandle> getImagesReliedOn() { return Arrays.asList(handle); }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((handle == null) ? 0 : handle.hashCode());
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
		HandleRenderSource other = (HandleRenderSource) obj;
		if (handle == null) {
			if (other.handle != null)
				return false;
		} else if (!handle.equals(other.handle))
			return false;
		return true;
	}

	@Override
	public RawImage render(RenderSettings settings) {
		try {
			RawImage img = HybridHelper.createImageNonNillable(settings.width, settings.height);
			
			GraphicsContext gc = img.getGraphics();
			
	//		g2.setRenderingHints(settings.hints);
			gc.scale( settings.width / (float)handle.getWidth(), 
					  settings.height / (float)handle.getHeight());
			handle.drawLayer( gc, null);
			
			return img;
    	} catch(InvalidImageDimensionsExeption e) {
    		return HybridHelper.createNillImage();
    	}
	}
}