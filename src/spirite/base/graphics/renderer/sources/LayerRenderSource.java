package spirite.base.graphics.renderer.sources;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.layers.Layer;
import spirite.hybrid.HybridHelper;

public class LayerRenderSource extends RenderSource {
		private final Layer layer;
		public LayerRenderSource( ImageWorkspace workspace, Layer layer) {
			super(workspace);
			this.layer = layer;
		}
		
		@Override public int getDefaultWidth() { return layer.getWidth(); }
		@Override public int getDefaultHeight() { return layer.getHeight(); }
		@Override public List<MediumHandle> getImagesReliedOn() {return layer.getImageDependencies(); }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((layer == null) ? 0 : layer.hashCode());
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
			LayerRenderSource other = (LayerRenderSource) obj;
			if (layer == null) {
				if (other.layer != null)
					return false;
			} else if (!layer.equals(other.layer))
				return false;
			return true;
		}

		@Override
		public RawImage render(RenderSettings settings) {
			try {
				RawImage img = HybridHelper.createImageNonNillable(settings.width, settings.height);
				
				GraphicsContext gc = img.getGraphics();
				
				gc.scale( settings.width / (float)layer.getWidth(),
						settings.height / (float)layer.getHeight());
				gc.translate(-layer.getDynamicOffsetX(), -layer.getDynamicOffsetY());
				
				layer.draw(gc);
				return img;
	    	} catch(InvalidImageDimensionsExeption e) {
	    		return HybridHelper.createNillImage();
	    	}
		}
	}