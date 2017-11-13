package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.MediumHandle;
import spirite.base.util.glmath.MatTrans;

public class BasePuppet implements IPuppet {
	BasePart rootPart = new BasePart();	// Nill part
	private final ImageWorkspace context;

	public BasePuppet( ImageWorkspace context, MediumHandle firstMedium) {
		this.context = context;
		rootPart.addPart(firstMedium);
	}

	@Override public BasePuppet getBase() { return this;}
	@Override
	public List<MediumHandle> getDependencies() {
		List<BasePart> parts = getParts();
		List<MediumHandle> list = new ArrayList<>(parts.size());
		
		for( BasePart part : parts)
			list.add(part.handle);
		
		return list;
	}
	@Override
	public List<BasePart> getParts() {
		List<BasePart> list = new ArrayList<>();

		Stack<BasePart> toCheckStack = new Stack<>();
		toCheckStack.push(rootPart);
		
		while(!toCheckStack.isEmpty()) {
			BasePart toCheck = toCheckStack.pop();
			if( toCheck.handle != null)
				list.add(toCheck);
			for( BasePart child : toCheck.children)
				toCheckStack.push(child);
		}
		
		return list;
	}
	@Override
	public List<TransformedHandle> getDrawList() {
		List<BasePart> parts = getParts();
		List<TransformedHandle> list = new ArrayList<>(parts.size());
		
		for( BasePart part : parts) {
			TransformedHandle th = new TransformedHandle();
			th.trans = MatTrans.TranslationMatrix(part.ox, part.oy);
			th.handle = part.handle;
			list.add(th);
		}
		
		
		return list;
	}
	
	public class BasePart implements IPuppet.IPart {
		BasePart parent;
		final List<BasePart> children = new ArrayList<>(2);
		
		int ox, oy;
		public MediumHandle handle;	// Note: must be maglev medium
		BaseBone bone;
		
		@Override
		public BuildingMediumData buildData() {
			return new BuildingMediumData(handle, ox, oy);
		}
		BasePart addPart(MediumHandle medium) {
			BasePart part = new BasePart();
			part.parent = this;
			part.handle = medium;
			this.children.add(part);
			
			return part;
		}
	}
	
	public static class BaseBone {
		public float x1, y1, x2, y2;

		public TreeMap<Float,Float> weightMap = new TreeMap<Float,Float>();
		
		public BaseBone(float width) {
			weightMap.put(0f, width);
			weightMap.put(1f, width);
		}
	}

	public IPart addNewPart() {
		MaglevMedium mlm = new MaglevMedium(context);
		
		
		IPart added = rootPart.addPart(context.importMedium(mlm));
		context.triggerFlash();
		return added;
	}


}
