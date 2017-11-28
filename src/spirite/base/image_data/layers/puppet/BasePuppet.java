package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import spirite.base.file.LoadEngine.PuppetPartInfo;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.util.linear.MatTrans;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;

public class BasePuppet implements IPuppet {
	BasePart rootPart = new BasePart(null);	// Nill part
	private final ImageWorkspace context;
	


	public BasePuppet( ImageWorkspace context, MediumHandle firstMedium) {
		this.context = context;
		rootPart._addPart(new BasePart(firstMedium));
	}

	public BasePuppet(ImageWorkspace context, List<PuppetPartInfo> toImport) {
		// Ugly, off-by-one math
		this.context = context;
		
		List<BasePart> partsAdded = new ArrayList<>(toImport.size()+1);
		partsAdded.add(rootPart);
		for( int i=0; i < toImport.size(); ++i) {
			PuppetPartInfo info = toImport.get(i);
			BasePart bp = new BasePart(info.handle);
			if( info.x1 != Float.NaN)
				bp.bone = new BaseBone(info.x1, info.y1, info.x2, info.y2, 1);
			bp.depth = info.depth;
			
			partsAdded.add(bp);
		}
		
		for( int i=1; i < partsAdded.size(); ++i) {
			PuppetPartInfo info = toImport.get(i-1);
			partsAdded.get(i).parent = partsAdded.get(info.parentId);
			partsAdded.get(i).parent.children.add(partsAdded.get(i));
		}
	}

	public BasePuppet(BasePuppet other) {
		this.context = other.context;
		
		for( BasePart child : other.rootPart.children) {
			BasePart copied = new BasePart(child, rootPart);
			rootPart.children.add(copied);
		}
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

	// ========
	// ==== Part Modification
	IPart addNewPart( int after) {
		_updatePartsDepth();
		
		MaglevMedium mlm = new MaglevMedium(context);
		MediumHandle handle = context.importMedium(mlm);
		BasePart part = new BasePart(handle);
		if( byDepthList == null || byDepthList.size() == 0 )
			part.depth = 0;
		else if( after < 0)
			part.depth = byDepthList.get(0).depth - 1;
		else if( after >= byDepthList.size())
			part.depth = byDepthList.get(byDepthList.size()-1).depth + 1;
		else
			part.depth = byDepthList.get(after).depth + 1;
		
		context.getUndoEngine().performAndStore(new UndoEngine.NullAction() {
			protected void performAction() {
				rootPart._addPart(part);
				context.triggerFlash();
			}
			protected void undoAction() {
				rootPart._removePart(part);
				context.triggerFlash();
			}
			public Collection<MediumHandle> getDependencies() {
				return Arrays.asList(new MediumHandle[] {handle});
			}
			public boolean reliesOnData() {
				return true;
			}
		});
		
		return part;
	}
	public void movePart(int from, int to) {
		_updatePartsDepth();
		
		if( from < 0 || from >= byDepthList.size() || to < 0 || to >= byDepthList.size()) {
			MDebug.handleWarning(WarningType.STRUCTURAL, "Attempted to move parts out of index.");
			return;
		}
		
		
		final BasePart part1 = byDepthList.get(from);
		final BasePart part2 = byDepthList.get(to);
		final int depth1 = part1.depth;
		final int depth2 = part2.depth;
		
		context.getUndoEngine().performAndStore(new UndoEngine.NullAction() {
			protected void performAction() {
				part1.depth = depth2;
				part2.depth = depth1;
				byDepthList = null;
				context.triggerFlash();
			}
			protected void undoAction() {
				part1.depth = depth1;
				part2.depth = depth2;
				byDepthList = null;
				context.triggerFlash();
			}
			public String getDescription() {
				return "Moving Puppet Part Depth";
			}
		});
	}
	
	// ==========
	// ==== Part List
	List<BasePart> byDepthList = null;
	
	@Override
	public List<BasePart> getParts() {
		_updatePartsDepth();
		
		return new ArrayList<>(byDepthList);
	}
	
	private void _updatePartsDepth() {
		if( byDepthList == null) {
			byDepthList = new ArrayList<>();
			

			Stack<BasePart> toCheckStack = new Stack<>();
			toCheckStack.push(rootPart);
			
			while(!toCheckStack.isEmpty()) {
				BasePart toCheck = toCheckStack.pop();
				if( toCheck.handle != null)
					byDepthList.add(toCheck);
				for( BasePart child : toCheck.children)
					toCheckStack.push(child);
			}
			
			byDepthList.sort((lhs,rhs) -> lhs.depth - rhs.depth);
			
			int minDepth = Integer.MIN_VALUE;
			for( BasePart bp : byDepthList) {
				if( bp.depth < minDepth)
					bp.depth = minDepth;
				
				minDepth = bp.depth+1;
			}
		}
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
		public final MediumHandle handle;	// Note: must be maglev medium
		BaseBone bone;
		int depth;
		
		BasePart( MediumHandle handle) {
			this.handle = handle;
		}
		
		public BasePart(BasePart other, BasePart parent) {
			this.handle = other.handle.dupe();
			this.parent = other.parent;
			this.ox = other.ox;
			this.oy = other.oy;
			this.bone = other.bone;	// Immutable
			this.depth = other.depth;
			
			for( BasePart child: other.children) {
				children.add(new BasePart(child,this));
			}
		}

		@Override
		public BuildingMediumData buildData() {
			return new BuildingMediumData(handle, ox, oy);
		}
		private void _addPart( BasePart part) {
			part.parent = this;
			children.add( part);
			byDepthList = null;
		}
		// Order probably doesn't matter as things are sorted by depth for accessing
//		private void _addPart( int index, BasePart part) {
//			
//			part.parent = this;
//			children.add(index, part);
//			byDepthList = null;
//		}
		private void _removePart( BasePart part) {
			if( children.remove(part))
				part.parent = null;
			byDepthList = null;
		}
		
		public BaseBone getBone() {return bone;}
		public BasePart getParent() {return parent;}
		public int getDepth() {return depth;}

		public void setBone(final BaseBone newBone) {
			final BaseBone oldBone = bone;
			
			context.getUndoEngine().performAndStore(new UndoEngine.NullAction() {
				protected void performAction() {
					bone = newBone;
				}
				protected void undoAction() {
					bone = oldBone;
				}
				public String getDescription() {
					return "Bone Creation / Modification";
				}
			});
		}
	}
	
	public static class BaseBone {
		public final float x1, y1, x2, y2;

		public TreeMap<Float,Float> weightMap = new TreeMap<Float,Float>();
		
		public BaseBone(float x1, float y1, float x2, float y2, float width) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			weightMap.put(0f, width);
			weightMap.put(1f, width);
		}
	}

	@Override
	public IPuppet dupe() {
		return new BasePuppet(this);
	}

	

}
