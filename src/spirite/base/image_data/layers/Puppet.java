package spirite.base.image_data.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import spirite.base.image_data.ImageHandle;
import spirite.base.util.glmath.Vec2;

public class Puppet {
	public final List<Part> parts = new ArrayList<>();
	
	public class Part {
		public final ImageHandle handle = null;
		BoneBase boneBase;
		Part parent = null;
		float origin_x, origin_y;
		float anchor_x, anchor_y;
		
	}
	
	public static class BoneBase {
		public float px1, py1;
		public float px2, py2;
		public TreeMap<Float,Float> weightMap = new TreeMap<Float,Float>();
		
		public BoneBase(float width) {
			weightMap.put(0f, width);
			weightMap.put(1f, width);
		}
	}
}
