package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.layers.puppet.BasePuppet.BasePart;
import spirite.base.util.interpolation.Interpolator2D;

public class Puppet implements IPuppet {
	BasePuppet base;
	Map<BasePart, Part> partMap = new HashMap<>();
	
	public Puppet( BasePuppet base) {
		this.base = base;
	}


	@Override public BasePuppet getBase() { return base;}
	@Override
	public List<MediumHandle> getDependencies() {
		return base.getDependencies();
	}
	@Override
	public List<? extends IPart> getParts() {
		return new ArrayList<>(partMap.values());
	}
	
	public class Part implements IPuppet.IPart {
		BasePart part;
		MediumHandle handle;	// Must be DerivedMaglevMedium derived from the part's MaglevMedium
		
		int ox, oy;
		
		@Override
		public BuildingMediumData buildData() {
			return new BuildingMediumData(handle, ox, oy);
		}
	}

	
	public class BoneState {
		float[] x;
		float[] y;
		
		Interpolator2D interpolator;
	}


	@Override
	public List<TransformedHandle> getDrawList() {
		return Arrays.asList(new TransformedHandle[0]);
	}

}
