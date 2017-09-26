package spirite.base.image_data.layers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.RawImage;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;


/**
 * 
 * 
 * @author Rory Burks
 *
 */
public class SpriteLayer extends Layer
{
	private final ArrayList<Part> parts = new ArrayList<>();
	private Part active = null;
	private ImageWorkspace context;
	int originX, originY;
	
	public SpriteLayer( ImageHandle handle) {
		context = handle.getContext();
		active = new Part(handle, "Base");
		parts.add( active);
	}
	
	public SpriteLayer( List<Part> parts) {
		this.parts.addAll(parts);
		
		if( !this.parts.isEmpty())
			active = this.parts.get(0);
		
		
		updateContext();
	}
	
	public static class Part {
		private final ImageHandle handle;
		private int ox, oy;
		private int depth;	
		private String partName;
		private boolean visible = true;
		private float alpha = 1.0f;
		
		public Part( ImageHandle handle, String pName, int ox, int oy, int depth, 
				boolean visible, float alpha) 
		{
			this.handle = handle;
			this.partName = pName;
			this.ox = ox;
			this.oy = oy;
			this.depth = depth;
			this.visible = visible;
			this.alpha = alpha;
		}
		
		private Part(ImageHandle handle, String partName) {
			this.handle = handle;
			this.partName = partName;
		}
		
		public int getDepth() {
			return depth;
		}
		public int getOffsetX() {
			return ox;
		}
		public int getOffsetY() {
			return oy;
		}
		public String getTypeName() {
			return partName;
		}
		public ImageHandle getImageHandle() {
			return handle;
		}
		public boolean isVisible() {
			return visible;
		}
		public float getAlpha() {
			return alpha;
		}
	}
	

	
	// :::: Unique Methods
	public Part getActivePart() {
		return active;
	}
	public void setActivePart(Part part) {
		if( parts.contains(part))
			active = part;
	}
	
	/** Returns the first highest-depth part that is visible and has 
	 * non-transparent data at x, y (in Layer-space)*/
	public Part grabPart(int x, int y, boolean select) {
		for( int i=parts.size()-1; i >= 0; --i) {
			Part part = parts.get(i);
			int dx = part.handle.getDynamicX();
			int dy = part.handle.getDynamicY();
			if( part.isVisible()) {
				if( !MUtil.coordInImage(x - part.ox-dx, y-part.oy-dy, part.handle.deepAccess()))
					continue;
				int rgb =part.handle.deepAccess().getRGB(x - part.ox-dx, y-part.oy-dy);
				
				if( ((rgb >>> 24) & 0xFF) == 0) continue; 
				
				if( select) {
					setActivePart(part);
					_trigger();
				}
				
				return part;
			}
		}
		return null;
	}
	
	/** Outside classes should use createModifyPartAction */
	private void _setPartDepth( Part part, int newDepth) {
		if( !parts.contains(part))
			return;

		if ( part.depth != newDepth) {
			part.depth = newDepth;
			
			parts.sort( new Comparator<Part>() {
				@Override
				public int compare(Part o1, Part o2) {
					return o1.depth - o2.depth;
				}
			});
		}
	}
	
	public List<Part> getParts() {
		return new ArrayList<>(parts);
	}
	
	/** Creates an UndoableAction representing the Creation of a new part.
	 * 
	 * This is the only way to create parts and outside of the ImageData
	 * namespace, it must be passed through the UndoEngine.performAndStoreAction
	 * method.*/
	public UndoableAction createAddPartAction(RawImage image, int ox, int oy, int depth, String partName) {
		updateContext();
		if( context == null)
			return null;
		
		if( parts.size() >= 255) {
			// Primarily for save/load simplicity
			MDebug.handleWarning(WarningType.UNSUPPORTED, this, "Only 255 parts per rig currently supported.");
			return null;
		}
		
		// Make sure you create a non-duplicate part name
		String testing = (partName == null || partName=="")?"_":partName;
		int i=0;
		boolean free = false;
		while( !free) {
			free = true;
			for( Part part : parts) {
				if( part.partName.equals(testing)) {
					testing = partName + "_" + (++i);
					free = false;
					break;
				}
			}
		}
		
		
		Part part = new Part(context.importDynamicData(image), testing);
		part.ox = ox;
		part.oy = oy;
		part.depth = depth;
		
		
		return new AddPartAction( part);
	}
	
	/** Removes an UndoableAction representing the Creation of a new part.
	 * 
	 * This is the only way to create parts and outside of the ImageData
	 * namespace, it must be passed through the UndoEngine.performAndStoreAction
	 * method.
	 * 
	 * Can return null if the part is not in the RigLayer*/
	public UndoableAction createRemovePartAction( Part part) {
		if( !parts.contains(part))
			return null;
		return new RemovePartAction(part);
	}

	/** Creates an action representing a change to a single Part's structure
	 * data (offset, name, etc)
	 * 
	 * This is the only way to change parts and outside of the ImageData
	 * namespace, it must be passed through the UndoEngine.performAndStoreAction
	 * method.
	 * 
	 * Can return null if the part is not in the RigLayer*/
	public UndoableAction createModifyPartAction( Part part, int ox, int oy, 
			int depth, String partName, boolean visible, float alpha) 
	{

		if( !parts.contains(part))
			return null;
		return new ChangePartAttributesAction(part, ox, oy, depth, partName, visible, alpha);
	}
	
	
	private void updateContext() {
		if( parts.isEmpty())
			return;
		
		context =  parts.get(0).handle.getContext();
	}
	

	
	
	// :::: Layer
	@Override
	public BuildingImageData getActiveData() {
		if( active == null)
			return null;
		
		
		return new BuildingImageData(active.handle, active.ox, active.oy);
	}

	@Override
	public List<BuildingImageData> getDataToBuild(){
		List<BuildingImageData> list = new ArrayList<>(parts.size());
		for( Part p : parts) {
			list.add( new BuildingImageData( p.handle, p.ox, p.oy));
		}
		return list;
	}

	@Override
	public List<ImageHandle> getImageDependencies() {
		List<ImageHandle> handles = new ArrayList<>( parts.size());
		for( Part part : parts) {
			handles.add(part.handle);
		}
		return handles;
	}

	@Override
	public void draw(GraphicsContext gc) {
		// Note: Parts are already pre-sorted by depth when they are added and when
		//	depth is changed.
		List<TransformedHandle> drawList = getDrawList();
		
		for( TransformedHandle th : drawList) {
			th.handle.drawLayer(gc, th.trans, th.comp, th.alpha);
		}
	}
	
	public void drawPart( GraphicsContext gc, Part part) {
		float oldAlpha = gc.getAlpha();
		Composite oldComp = gc.getComposite();
		
		gc.setComposite(Composite.SRC_OVER, oldAlpha*part.alpha);
		
		MatTrans trans = new MatTrans();
		trans.translate(part.ox,part.oy);
		part.handle.drawLayer( gc, trans);
		
		gc.setComposite(oldComp, oldAlpha);
	}
	

	@Override
	public int getWidth() {
		int x1 = 0;
		int x2 = 0;
		
		for( Part part : parts) {
			if( part.ox < x1) x1 = part.ox;
			
			int px2 = part.ox + part.handle.getWidth();
			if( px2 > x2) x2 = px2;
		}
		
		return x2 - 0;
	}

	@Override
	public int getHeight() {
		int y1 = 0;
		int y2 = 0;
		
		for( Part part : parts) {
			if( part.oy < y1) y1 = part.oy;
			
			int py2 = part.oy + part.handle.getHeight();
			if( py2 > y2) y2 = py2;
		}
		return y2 - 0;
	}

	@Override
	public boolean canMerge(Node node) {
		return node instanceof LayerNode;
	}

	@Override
	public LayerActionHelper merge(Node node, int x, int y) {
		Layer layer = ((LayerNode)node).getLayer();
		if( layer instanceof SpriteLayer) {
			SpriteLayer other = (SpriteLayer)layer;
	
			// TODO:
//			Merge
//			for( )
		}
		
		return null;
	}

	@Override
	public List<Rect> getBoundList() {
		List<Rect> list = new ArrayList<>(parts.size());
		
		for( Part part : parts) {
			list.add( new Rect(part.ox, part.oy, part.handle.getWidth(), part.handle.getHeight()));
		}
		
		return list;
	}

	@Override
	public Layer logicalDuplicate() {
		List<Part> dupeParts = new ArrayList<Part>(parts.size());
		for( Part part : parts) {
			dupeParts.add(new Part(
				part.handle.dupe(),
				part.partName,
				part.ox,
				part.oy,
				part.depth,
				part.visible,
				part.alpha
			));
		}
		return new SpriteLayer( dupeParts);
	}
	

	@Override
	public List<TransformedHandle> getDrawList() {
		List<TransformedHandle> list = new ArrayList<> ( parts.size());
		
		for( Part part : parts) {
			if( part.isVisible()) {
				
				TransformedHandle renderable = new TransformedHandle();

				renderable.comp = Composite.SRC_OVER;
				renderable.alpha = part.alpha;
				
				renderable.handle = part.handle;
				renderable.trans.translate( part.ox, part.oy);
				renderable.depth = part.depth;
				list.add(renderable);
			}
		}
		
		return list;
	}
	
	
	// :::: Rig Modification related 
	public class AddPartAction extends NullAction {
		private final Part added;
		
		AddPartAction( Part part) {
			added = part;
			this.description = "Added Part to Rig";
		}
		
		public Part getPart() {
			return added;
		}
		@Override
		protected void performAction() {
			parts.add( added);
			active = added;
			parts.sort( new Comparator<Part>() {
				@Override
				public int compare(Part o1, Part o2) {
					return o1.depth - o2.depth;
				}
			});
			_trigger();
		}
		@Override
		protected void undoAction() {
			if (active == added)
				active = null;
			parts.remove(added);
			_trigger();
		}
		@Override public boolean reliesOnData() {return true;}
		@Override
		public Collection<ImageHandle> getDependencies() {
			return Arrays.asList(new ImageHandle[] {added.handle});
		}
	}
	public class RemovePartAction extends NullAction {
		private final Part removed;
		
		RemovePartAction( Part part) {
			removed = part;
			this.description = "Removed Part from Rig";
		}
		@Override
		protected void performAction() {
			if (active == removed)
				active = null;
			parts.remove(removed);
			_trigger();
		}
		@Override
		protected void undoAction() {
			parts.add( removed);
			parts.sort( new Comparator<Part>() {
				@Override
				public int compare(Part o1, Part o2) {
					return o1.depth - o2.depth;
				}
			});	
			_trigger();
		}
		@Override public boolean reliesOnData() {return true;}
		@Override
		public Collection<ImageHandle> getDependencies() {
			return Arrays.asList(new ImageHandle[] {removed.handle});
		}
	}

	public class ChangePartAttributesAction extends NullAction 
		implements StackableAction
	{
		private final Part part;
		private int old_ox, old_oy, new_ox, new_oy;
		private int old_depth, new_depth;
		private String oldName, newName;
		private boolean oldVisible, newVisible;
		private float oldAlpha, newAlpha;
		
		ChangePartAttributesAction( Part part, int ox, int oy, int depth, String name,
				boolean visible, float alpha) 
		{
			this.part = part;
			this.old_ox = part.ox;
			this.old_oy = part.oy;
			this.old_depth = part.depth;
			this.oldName = part.partName;
			this.oldAlpha = part.alpha;
			this.oldVisible = part.visible;
			this.new_ox = ox;
			this.new_oy = oy;
			this.new_depth = depth;
			this.newName = name;
			this.newAlpha = alpha;
			this.newVisible = visible;
		}
		
		@Override
		protected void performAction() {
			part.ox = new_ox;
			part.oy = new_oy;
			_setPartDepth(part, new_depth);
			part.partName = newName;
			part.visible = newVisible;
			part.alpha = newAlpha;
			_trigger();
		}

		@Override
		protected void undoAction() {
			part.ox = old_ox;
			part.oy = old_oy;
			_setPartDepth(part, old_depth);
			part.partName = oldName;
			part.visible = oldVisible;
			part.alpha = oldAlpha;
			_trigger();
		}

		@Override
		public void stackNewAction(UndoableAction newAction) {
			if( canStack(newAction));
			
			ChangePartAttributesAction other = (ChangePartAttributesAction)newAction;
			this.new_depth = other.new_depth;
			this.new_ox = other.new_ox;
			this.new_oy = other.new_oy;
			this.newName = other.newName;
		}

		@Override
		public boolean canStack(UndoableAction newAction) {
			return ((newAction instanceof ChangePartAttributesAction) &&
				((ChangePartAttributesAction)newAction).part == part);
		}
		
	}
	
	private void _trigger() {
		updateContext();
		if( context != null) 
			context.triggerInternalLayerChange(this);
		triggerStructureChange();
	}
	
	// :::: Observer
	public interface RigStructureObserver {
		public void rigStructureChanged();
	}
	private final List<RigStructureObserver> rigObservers = new ArrayList<>(1);
	public void addRigObserver( RigStructureObserver obs) { rigObservers.add(obs);}
	public void removeRigObserver( RigStructureObserver obs) { rigObservers.remove(obs);}
	private void triggerStructureChange() {
		for( RigStructureObserver obs : rigObservers) {
			obs.rigStructureChanged();
		}
	}

	@Override
	public LayerActionHelper interpretCrop( List<ImageCropHelper> crops) {
		LayerActionHelper helper = null;
		
		for( ImageCropHelper crop : crops) {
			for( Part part : parts) {
				if( crop.handle.equals(part.handle)) {
					if( helper == null)
						helper = new LayerActionHelper();
					
					helper.actions.add( new ChangePartAttributesAction(
							part, part.ox + crop.dx, part.oy + crop.dy, 
							part.depth, part.partName, part.visible, part.alpha));
				}
			}
		}
		
		return helper;
	}

	@Override
	public int getDynamicOffsetX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDynamicOffsetY() {
		// TODO Auto-generated method stub
		return 0;
	}
}
