package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.layers.puppet.BasePuppet.BasePart;
import spirite.base.util.interpolation.Interpolator2D;

public class Puppet {
	BasePuppet base;
	Map<BasePart, Part> partMap = new HashMap<>();
	
	public class Part {
		BasePart part;
		
	}
	
	public List<MediumHandle> getDependencies() {
		return base.getDependencies();
	}
	
	public class BoneState {
		float[] x;
		float[] y;
		
		Interpolator2D interpolator;
	}
}
