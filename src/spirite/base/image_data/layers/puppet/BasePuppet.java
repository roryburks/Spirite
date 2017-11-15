package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.UndoEngine;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.MDebug.WarningType;
import spirite.hybrid.MDebug;

public class BasePuppet implements IPuppet {
	BasePart rootPart = new BasePart(null);	// Nill part
	private final ImageWorkspace context;
	


	public BasePuppet( ImageWorkspace context, MediumHandle firstMedium) {
		this.context = context;
		rootPart._addPart(new BasePart(firstMedium));
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
		
		@Override
		public BuildingMediumData buildData() {
			return new BuildingMediumData(handle, ox, oy);
		}
		private void _addPart( BasePart part) {
			part.parent = this;
			children.add( part);
			byDepthList = null;
		}
		private void _addPart( int index, BasePart part) {
			
			part.parent = this;
			children.add(index, part);
			byDepthList = null;
		}
		private void _removePart( BasePart part) {
			if( children.remove(part))
				part.parent = null;
			byDepthList = null;
		}
//		protected BasePart addPart(MediumHandle medium) {
//			final BasePart part = new BasePart();
//			part.parent = this;
//			part.handle = medium;
//			
//			context.getUndoEngine().performAndStore(new UndoEngine.NullAction() {
//				@Override
//				protected void performAction() {
//					BasePart.this.children.add(part);
//					byDepthList = null;
//				}
//				
//				@Override
//				protected void undoAction() {
//					BasePart.this.children.remove(part);
//					byDepthList = null;
//				}
//				
//				@Override
//				public Collection<MediumHandle> getDependencies() {
//					return Arrays.asList(new MediumHandle[] {medium});
//				}
//			});
//			
//			return part;
//		}
	}
	
	public static class BaseBone {
		public float x1, y1, x2, y2;

		public TreeMap<Float,Float> weightMap = new TreeMap<Float,Float>();
		
		public BaseBone(float width) {
			weightMap.put(0f, width);
			weightMap.put(1f, width);
		}
	}

	

}
