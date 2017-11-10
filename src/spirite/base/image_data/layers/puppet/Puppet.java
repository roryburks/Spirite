package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.layers.puppet.BasePuppet.BasePart;
import spirite.base.image_data.mediums.maglev.DerivedMaglevMedium;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.util.interpolation.Interpolator2D;

public class Puppet implements IPuppet {
	private final ImageWorkspace workspace;
	BasePuppet base;
	Map<BasePart, Part> partMap = new HashMap<>();
	
	public Puppet( BasePuppet base, ImageWorkspace workspace) {
		this.workspace = workspace;
		this.base = base;
	}
	
	private void updateMap() {
		// Step 1: Remove deleted BaseParts (as marked by setting their parent to null)
		Iterator<BasePart> it = partMap.keySet().iterator();
		while( it.hasNext()) {
			BasePart part = it.next();
			if( part.parent == null)
				it.remove();
		}
		
		// Step 2: Add un-mapped Parts
		for( BasePart part : base.getParts()) {
			if( !partMap.containsKey(part)) 
				partMap.put(part, new Part( part));
		}
	}


	// ======
	// ==== IPuppet Interface
	@Override public BasePuppet getBase() { return base;}
	@Override
	public List<MediumHandle> getDependencies() {
		updateMap();
		List<MediumHandle> list = new ArrayList<>();
		list.addAll(base.getDependencies());
		
		for( Part part : partMap.values())
			list.add(part.handle);
		
		return list;
	}
	@Override
	public List<? extends IPart> getParts() {
		updateMap();
		return new ArrayList<>(partMap.values());
	}
	
	public class Part implements IPuppet.IPart {
		final BasePart base;
		MediumHandle handle;	// Must be DerivedMaglevMedium derived from the part's MaglevMedium
		BoneState state;
		
		int ox, oy;
		
		Part( BasePart base) {
			this.base = base;
			MaglevMedium baseMedium = (MaglevMedium) workspace.getData(base.hadle);
			handle = workspace.importMedium( new DerivedMaglevMedium(baseMedium));
		}
		
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
