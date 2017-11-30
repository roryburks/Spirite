package spirite.base.image_data.layers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.UndoEngine.NullAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.mediums.DynamicMedium;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.MatTrans.NoninvertableException;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;
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
	
	public SpriteLayer( MediumHandle handle) {
		context = handle.getContext();
		active = new Part(new PartStructure(handle, "Base"));
		parts.add( active);
	}
	
	public SpriteLayer( List<PartStructure> structures) {
		
		for( PartStructure pstruct : structures) {
			this.parts.add( new Part( new PartStructure( pstruct)));
		}
		
		if( !this.parts.isEmpty())
			active = this.parts.get(0);
		
		
		updateContext();
	}
	
	public static class PartStructure {
		public MediumHandle handle;
		public int depth;
		public String partName;
		public boolean visible = true;
		public float alpha = 1.0f;
		public float transX, transY;
		public int ox, oy;
		public float scaleX = 1.0f;
		public float scaleY = 1.0f;
		public float rot;
		
		public PartStructure() {}
		private PartStructure(MediumHandle handle, String partName) {
			this( handle, partName, 0, 0, 0, 0, 1, 1, 0, 0, true, 1);
		}
		public PartStructure( 
				MediumHandle handle, String pName, int ox, int oy, 
				int depth, boolean visible, float alpha) 
		{
			this( handle, pName, ox, oy, 0, 0, 1, 1, 0, depth, visible, alpha);
		}
		public PartStructure( 
				MediumHandle handle, String pName, int ox, int oy,
				float transX, float transY, float scaleX, float scaleY, float rotation,
				int depth, boolean visible, float alpha) 
		{
			this.handle = handle;
			this.partName = pName;
			this.ox = ox;
			this.oy = oy;
			this.transX = transX;
			this.transY = transY;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.rot = rotation;
			this.depth = depth;
			this.visible = visible;
			this.alpha = alpha;
		}
		public PartStructure( PartStructure other) {
			copyFrom( other);
		}
		
		public void copyFrom( PartStructure other) {
			this.handle = other.handle;
			this.partName = other.partName;
			this.transX = other.transX;
			this.transY = other.transY;
			this.scaleX = other.scaleX;
			this.scaleY = other.scaleY;
			this.ox = other.ox;
			this.oy = other.oy;
			this.rot = other.rot;
			this.depth = other.depth;
			this.visible = other.visible;
			this.alpha = other.alpha;
		}
	}
	public class Part {
		private final PartStructure structure = new PartStructure();
		
		private Part( PartStructure structure) {
			this.structure.copyFrom(structure);
		}
		
		public int getDepth() { return structure.depth;}
		public float getTranslationX() {return structure.transX;}
		public float getTranslationY() {return structure.transY;}
		public float getScaleX() {return structure.scaleX;}
		public float getScaleY() {return structure.scaleY;}
		public float getRotation() {return structure.rot;}
		public String getTypeName() {return structure.partName;}
		public MediumHandle getImageHandle() {return structure.handle;}
		public boolean isVisible() {return structure.visible;}
		public float getAlpha() {return structure.alpha;}
		public SpriteLayer getContext() {return SpriteLayer.this;}
		public PartStructure getStructure() {return new PartStructure(structure);}
		public MatTrans buildTransform() {
			MatTrans ret = new MatTrans();
			ret.preTranslate(-structure.handle.getWidth()/2, -structure.handle.getHeight()/2);
			ret.preScale(structure.scaleX, structure.scaleY);
			ret.preRotate(structure.rot);
			ret.preTranslate(structure.transX+structure.handle.getWidth()/2, 
					structure.transY+structure.handle.getHeight()/2);
			return ret;
		}
		public MatTrans buildInverseTransform() {
			try {
				return buildTransform().createInverse();
			} catch (NoninvertableException e) {
				e.printStackTrace();
				return new MatTrans();
			}
		}
		public int _getOX() {
			return (int)structure.transX;
		}
		public int _getOY() {
			return (int)structure.transY;
		}
	}
	

	
	// :::: Unique Methods
	public Part getActivePart() {
		return active;
	}
	public int getActivePartIndex() {
		return parts.indexOf(active);
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
			int dx = part.structure.handle.getDynamicX();
			int dy = part.structure.handle.getDynamicY();
			if( part.isVisible()) {
				MatTrans invTrans = part.buildInverseTransform();
				Vec2 from = new Vec2( x - dx, y-dy);
				Vec2 to = invTrans.transform(from);
				
				if( !MUtil.coordInImage( (int)to.x, (int)to.y, part.structure.handle.deepAccess()))
					continue;
				int rgb =part.structure.handle.deepAccess().getRGB((int)to.x, (int)to.y);
				
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
	private void _sortByDepth() {
		parts.sort( (o1, o2) -> {
			return o1.structure.depth - o2.structure.depth;
		});
	}
	
	public List<Part> getParts() {
		return new ArrayList<>(parts);
	}
	
	/** Creates an UndoableAction representing the Creation of a new part.
	 * 
	 * This is the only way to create parts and outside of the ImageData
	 * namespace, it must be passed through the UndoEngine.performAndStoreAction
	 * method.*/
	public void addPart( RawImage image, String partName) {
		updateContext();
		PartStructure struct = new PartStructure( context.importMedium( new DynamicMedium(image, 0, 0, context))
				, partName, 0, 0, 0, true, 1);
		addPart( struct, (active == null) ? parts.size() : parts.indexOf(active) + 1);
	}
	private void addPart( PartStructure structure, int index) {
		if( context == null)
			return;

		if( parts.size() >= 255) {
			// Primarily for save/load simplicity
			MDebug.handleWarning(WarningType.UNSUPPORTED, this, "Only 255 parts per rig currently supported.");
			return;
		}
		
		UndoEngine undoEngine = context.getUndoEngine();
		PartStructure newStructure = new PartStructure(structure);
		
		
		undoEngine.doAsAggregateAction(() -> {
			// Note: Because of how doAsAggregateAction prevents action until it's completed,
			//	the logic in this method is counter-intuitively valid, as all of the checks
			//	are done before the new part gets added, even though it's done using 
			//	a performAndStore that happens before them.  Probably bad design.
			
			int dlow = Math.max( newStructure.depth, 
					(index == 0) ? -Integer.MIN_VALUE : parts.get(index-1).structure.depth+1);
			
			List<String> names = new ArrayList<>(parts.size());
			for( Part part : parts)
				names.add(part.getTypeName());
			String name = MUtil.getNonDuplicateName(names, newStructure.partName);
			
			newStructure.partName = name;
			newStructure.depth = dlow++;
			
			final Part toAdd = new Part( newStructure);

			undoEngine.performAndStore( new NullAction() {
				protected void performAction() {
					parts.add( index, toAdd);
					active = toAdd;
					_sortByDepth();
					_trigger();
				}
				protected void undoAction() {
					if (active == toAdd)
						active = null;
					parts.remove(toAdd);
					_trigger();
				}
				public boolean reliesOnData() {return true;}
				public Collection<MediumHandle> getDependencies() {
					return Arrays.asList(new MediumHandle[] {toAdd.structure.handle});
				}
			});
			
			// Bubble up the depth of anything that matches this depth
			for( int i=index; i < parts.size(); ++i) {
				PartStructure structureToChange = parts.get(i).getStructure();
				if( structureToChange.depth < dlow) {
					structureToChange.depth = dlow++;
					modifyPart(parts.get(i), structureToChange);
				}
				else 
					break;
			}
			
		}, "Add New Part");
		
		if( active == null) {
			
		}
	}
	
	/** Removes an UndoableAction representing the Creation of a new part.
	 * 
	 * This is the only way to create parts and outside of the ImageData
	 * namespace, it must be passed through the UndoEngine.performAndStoreAction
	 * method.
	 * 
	 * Can return null if the part is not in the RigLayer*/
	public void removePart( final Part toRemove) {
		if( !parts.contains(toRemove))
			return;
		
		context.getUndoEngine().performAndStore(new NullAction() {
			protected void performAction() {
				if (active == toRemove)
					active = null;
				parts.remove(toRemove);
				_trigger();
			}
			
			protected void undoAction() {
				parts.add( toRemove);
				parts.sort( new Comparator<Part>() {
					@Override
					public int compare(Part o1, Part o2) {
						return o1.structure.depth - o2.structure.depth;
					}
				});	
				_trigger();
			}
			
			public String getDescription() { return "Removed Part from Rig";}
			public boolean reliesOnData() {return true;}
			public Collection<MediumHandle> getDependencies() {
				return Arrays.asList(new MediumHandle[] {toRemove.structure.handle});
			}
		});
	}
	

	/** Moves the part at from and moves it such that it index is to */
	public void movePart(int fromInd, int toInd) {
		UndoEngine undoEngine = context.getUndoEngine();

		Part toMove = parts.get(fromInd);
		undoEngine.doAsAggregateAction(() -> {
			removePart(toMove);
			
			addPart( toMove.structure, toInd);
		}, "Move Part");
	}

	/** Creates an action representing a change to a single Part's structure
	 * data (offset, name, etc)
	 * 
	 * This is the only way to change parts and outside of the ImageData
	 * namespace, it must be passed through the UndoEngine.performAndStoreAction
	 * method.
	 * 
	 * Can return null if the part is not in the RigLayer*/
	public void modifyPart( 
			Part part, PartStructure newStructure) 
	{
		updateContext();
		if( !parts.contains(part))
			return;
		
		context.getUndoEngine().performAndStore(new ChangePartAttributesAction( part, newStructure));
	}
	public class ChangePartAttributesAction extends NullAction 
		implements StackableAction
	{
		private final Part part;
		private final PartStructure oldStructure;
		private final PartStructure newStructure;
		ChangePartAttributesAction( 
				Part part, PartStructure newStructure) 
		{
			this.part = part;
			this.oldStructure = new PartStructure(part.structure);
			this.newStructure = new PartStructure( newStructure);
		}
		
		@Override
		protected void performAction() {
			part.structure.copyFrom(newStructure);
			_sortByDepth();
			_trigger();
		}
	
		@Override
		protected void undoAction() {
			part.structure.copyFrom(oldStructure);
			_sortByDepth();
			_trigger();
		}
	
		@Override
		public void stackNewAction(UndoableAction newAction) {
			if( canStack(newAction));
			
			ChangePartAttributesAction other = (ChangePartAttributesAction)newAction;
			this.newStructure.copyFrom(other.newStructure);
		}
	
		@Override
		public boolean canStack(UndoableAction newAction) {
			return ((newAction instanceof ChangePartAttributesAction) &&
				((ChangePartAttributesAction)newAction).part == part);
		}
		
	}
	
	
	private void updateContext() {
		if( parts.isEmpty())
			return;
		
		context =  parts.get(0).structure.handle.getContext();
	}
	

	
	
	// :::: Layer
	@Override
	public BuildingMediumData getActiveData() {
		if( active == null)
			return null;
		
		return new BuildingMediumData(active.structure.handle, active._getOX(), active._getOY());
	}
	@Override
	public IImageDrawer getDrawer(BuildingMediumData building, IMedium medium) {
		return medium.getImageDrawer(building);
	}

	@Override
	public List<BuildingMediumData> getDataToBuild(){
		List<BuildingMediumData> list = new ArrayList<>(parts.size());
		for( Part p : parts) {
			list.add( new BuildingMediumData( p.structure.handle, p._getOX(), p. _getOY()));
		}
		return list;
	}

	@Override
	public List<MediumHandle> getImageDependencies() {
		List<MediumHandle> handles = new ArrayList<>( parts.size());
		for( Part part : parts) {
			handles.add(part.structure.handle);
		}
		return handles;
	}
	
	public void drawPart( GraphicsContext gc, Part part) {
		float oldAlpha = gc.getAlpha();
		Composite oldComp = gc.getComposite();
		
		gc.setComposite(Composite.SRC_OVER, oldAlpha*part.structure.alpha);
		
		MatTrans trans = new MatTrans();
		trans.translate(part._getOX(),part._getOY());
		part.structure.handle.drawLayer( gc, trans);
		
		gc.setComposite(oldComp, oldAlpha);
	}
	

	@Override public int getDynamicOffsetX() {return 0;}
	@Override public int getDynamicOffsetY() {return 0;}
	@Override
	public int getWidth() {
		int x1 = 0;
		int x2 = 0;
		
		for( Part part : parts) {
			if( part._getOX() < x1) x1 = part._getOY();
			
			int px2 = part._getOX() + part.structure.handle.getWidth();
			if( px2 > x2) x2 = px2;
		}
		
		return x2 - 0;
	}

	@Override
	public int getHeight() {
		int y1 = 0;
		int y2 = 0;
		
		for( Part part : parts) {
			if( part._getOY() < y1) y1 = part._getOY();
			
			int py2 = part._getOY() + part.structure.handle.getHeight();
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
			list.add( new Rect(part._getOX(), part._getOY(), part.structure.handle.getWidth(), part.structure.handle.getHeight()));
		}
		
		return list;
	}

	@Override
	public Layer logicalDuplicate() {
		List<PartStructure> dupeParts = new ArrayList<PartStructure>(parts.size());
		for( Part part : parts) {
			PartStructure structure = new PartStructure(part.structure);
			dupeParts.add(structure);
		}
		return new SpriteLayer( dupeParts);
	}
	

	@Override
	public List<TransformedHandle> getDrawList() {
		List<TransformedHandle> list = new ArrayList<> ( parts.size());
		
		for( Part part : parts) {
			if( part.isVisible()) {
				
				TransformedHandle renderable = new TransformedHandle();

				renderable.alpha = part.structure.alpha;
				
				renderable.handle = part.structure.handle;
				renderable.trans = part.buildTransform();
				renderable.depth = part.structure.depth;
				list.add(renderable);
			}
		}
		
		return list;
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
				if( crop.handle.equals(part.structure.handle)) {
					if( helper == null)
						helper = new LayerActionHelper();
					
					// TODO
//					helper.actions.add( new ChangePartAttributesAction(
//							part, part.ox + crop.dx, part.oy + crop.dy, 
//							part.depth, part.partName, part.visible, part.alpha));
				}
			}
		}
		
		return helper;
	}

}
