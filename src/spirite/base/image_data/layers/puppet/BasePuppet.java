package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.mediums.maglev.MaglevMedium;

public class BasePuppet {
	BasePart rootPart;	// Nill part

	public List<MediumHandle> getDependencies() {
		List<MediumHandle> list = new ArrayList<>();
		
		Stack<BasePart> toCheckStack = new Stack<>();
		toCheckStack.push(rootPart);
		
		while(!toCheckStack.isEmpty()) {
			BasePart toCheck = toCheckStack.pop();
			if( toCheck.medium != null)
				list.add(toCheck.medium);
		}
		
		return list;
	}
	
	public class BasePart {
		BasePart parent;
		final List<BasePart> children = new ArrayList<>(2);
		
		int ox, oy;
		MediumHandle medium;	// Note: must be maglev medium
		BaseBone bone;
	}
	
	public class BaseBone {
		int x1, y1, x2, y2;

		public TreeMap<Float,Float> weightMap = new TreeMap<Float,Float>();
		
		public BaseBone(float width) {
			weightMap.put(0f, width);
			weightMap.put(1f, width);
		}
	}
}
