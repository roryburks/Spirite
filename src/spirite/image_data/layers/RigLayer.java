package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuildingImageData;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.UndoEngine.NullAction;
import spirite.image_data.UndoEngine.StackableAction;
import spirite.image_data.UndoEngine.UndoableAction;


/**
 * A Rig is composed of multiple Images arranged in parts, each part having an 
 * offset and possibly more.
 * 
 * 
 * @author Rory Burks
 *
 */
public class RigLayer extends Layer
{
	private final List<Part> parts = new ArrayList<>();
	private Part active = null;
	private ImageWorkspace context;
	
	public RigLayer( ImageHandle handle) {
		context = handle.getContext();
		active = new Part(handle, "Base");
		parts.add( active);
	}
	
	public RigLayer( List<Part> parts) {
		this.parts.addAll(parts);
		
		if( !this.parts.isEmpty())
			active = this.parts.get(0);
		
		
		updateContext();
	}
	
	public static class Part {
		private final ImageHandle handle;
		private int ox, oy;
		// TODO: Make this global, not just local, i.e. having a depth of -1 will draw
		//	before everything with a greater depth, even outside of this rig 
		//	(but probably limited to the same group?)
		private int depth;	
		private String partName;
		
		public Part( ImageHandle handle, String pName, int ox, int oy, int depth) {
			this.handle = handle;
			this.partName = pName;
			this.ox = ox;
			this.oy = oy;
			this.depth = depth;
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
	}
	

	
	// :::: Unique Methods
	public Part getActivePart() {
		return active;
	}
	public void setActivePart(Part part) {
		if( parts.contains(part))
			active = part;
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
	public UndoableAction createAddPartAction(BufferedImage image, int ox, int oy, int depth, String partName) {
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
		
		
		Part part = new Part(context.importData(image), testing);
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
	public UndoableAction createModifyPartAction( 
			Part part, int ox, int oy, int depth, String partName) 
	{

		if( !parts.contains(part))
			return null;
		return new ChangePartAttributesAction(part, ox, oy, depth, partName);
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
	public List<ImageHandle> getUsedImages() {
		List<ImageHandle> handles = new ArrayList<>( parts.size());
		for( Part part : parts) {
			handles.add(part.handle);
		}
		return handles;
	}

	@Override
	public void draw(Graphics g) {
		// Note: Parts are already pre-sorted by depth when they are added and when
		//	depth is changed.
		Graphics2D g2 = (Graphics2D)g;
		
		AffineTransform trans = g2.getTransform();
		
		for( Part part : parts) {
			g2.translate(part.ox, part.oy);
			part.handle.drawLayer(g);
			g2.setTransform(trans);
		}
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
		
		return x2 - x1;
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
		return y2 - y1;
	}

	@Override
	public boolean canMerge(Node node) {
		return node instanceof LayerNode;
	}

	@Override
	public MergeHelper merge(Node node, int x, int y) {
		Layer layer = ((LayerNode)node).getLayer();
		if( layer instanceof RigLayer) {
			RigLayer other = (RigLayer)layer;
	
			// TODO:
//			Merge
//			for( )
		}
		
		return null;
	}

	@Override
	public List<Rectangle> interpretCrop(Rectangle rect) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Layer logicalDuplicate() {
		// TODO
		return new RigLayer( parts.get(0).handle);
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
			parts.remove(added);
			if (active == added)
				active = null;
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
			this.description = "Added Part to Rig";
		}
		@Override
		protected void performAction() {
			parts.remove(removed);
			if (active == removed)
				active = null;
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
		
		ChangePartAttributesAction( Part part, int ox, int oy, int depth, String name ) 
		{
			this.part = part;
			this.old_ox = part.ox;
			this.old_oy = part.oy;
			this.old_depth = part.depth;
			this.oldName = part.partName;
			this.new_ox = ox;
			this.new_oy = oy;
			this.new_depth = depth;
			this.newName = name;
		}
		
		@Override
		protected void performAction() {
			part.ox = new_ox;
			part.oy = new_oy;
			_setPartDepth(part, new_depth);
			part.partName = newName;
			_trigger();
		}

		@Override
		protected void undoAction() {
			part.ox = old_ox;
			part.oy = old_oy;
			_setPartDepth(part, old_depth);
			part.partName = oldName;
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

}
