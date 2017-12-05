package spirite.base.image_data.layers.puppet;

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.MediumHandle;

import java.util.Collection;
import java.util.List;

public interface IPuppet {
	public interface IPart {
		public BuildingMediumData buildData();
	}

	public Collection<MediumHandle> getDependencies();
	public List<? extends IPart> getParts();
	public List<TransformedHandle> getDrawList();
	public BasePuppet getBase();
	public IPuppet dupe();
}
