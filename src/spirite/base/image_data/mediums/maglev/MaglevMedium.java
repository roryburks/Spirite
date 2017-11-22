package spirite.base.image_data.mediums.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.DynamicImage;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.MatTrans.NoninvertableException;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.interpolation.Interpolator2D;
import spirite.hybrid.HybridHelper;

/**
 * A Maglev Internal Image is an image that floats just above the surface, 
 * not quite planting its feat in the ground.  Essentially it is a kind of 
 * Scalable Vector Image, storing all the different stroke and fill actions
 * as logical vertex data rather than rendered pixel data, allowing them to
 * be scaled/rotated without generation loss	.
 */
public class MaglevMedium implements IMedium {
	//final List<MagLevStroke> strokes;
	//final List<MagLevFill> fills;
	List<AMagLevThing> things;
	
	final ImageWorkspace context;
	DynamicImage builtImage = null;
	private boolean isBuilt = false;
	
	public MaglevMedium( ImageWorkspace context) {
		this.context = context;
		this.things =  new ArrayList<>();
		if( context != null) {
			// TODO: unflag "isBuilt" when Workspace changes size
		}
	}
	public MaglevMedium( ImageWorkspace context, List<AMagLevThing> things) {
		this.context = context;
		this.things = new ArrayList<>( things.size());
		this.things.addAll(things);
		
		for( AMagLevThing thing : things)
			thing.id = workingId++;
	}
	private MaglevMedium( MaglevMedium other) {
		this.context = other.context;
		this.things = new ArrayList<>(other.things.size());
		
		for( AMagLevThing thing : other.things)
			this.things.add(thing.clone());
		
		this.isBuilt = other.isBuilt;
		this.builtImage = (other.builtImage == null) ? null : other.builtImage.deepCopy();
	}
	
	public ImageWorkspace getContext() {return context;}
	

	int workingId = 0;
	void addThing( AMagLevThing thing, boolean back) {
		thing.id = workingId++;
		if( back)
			things.add(0, thing);
		else
			things.add(thing);
	}
	
	// ==== Easy Junk
	@Override public int getWidth() {return (isBuilt) ? builtImage.getWidth() : context.getWidth(); }
	@Override public int getHeight() {return (isBuilt) ? builtImage.getHeight() : context.getHeight();}

	@Override public int getDynamicX() { return (isBuilt) ? builtImage.getXOffset() : 0; }
	@Override public int getDynamicY() { return (isBuilt) ? builtImage.getYOffset() : 0; }

	@Override public ABuiltMediumData build(BuildingMediumData building) {return new MaglevBuiltData(building);}
	@Override public IImageDrawer getImageDrawer(BuildingMediumData building) 
		{return new MaglevImageDrawer(this, building);}

	@Override public IMedium dupe() {return new MaglevMedium(this);}
	@Override public IMedium copyForSaving() {return new MaglevMedium(this);}
	@Override public InternalImageTypes getType() {return InternalImageTypes.MAGLEV;}

	@Override public IImage readOnlyAccess() {
		Build();
		return builtImage.getBase();
	}
	
	@Override
	public void flush() {
		if( builtImage != null)
			builtImage.flush();
		builtImage = null;
		isBuilt = false;
		//this.strokes.clear();
	}


	// ==== Hard Junk
	boolean building = false;
	void Build() {
		if( !building) {
			building = true;
			if( !isBuilt) {
				builtImage = new DynamicImage(context, HybridHelper.createNillImage(), 0, 0);
				
				builtImage.doOnGC( (gc) -> {
					ABuiltMediumData built = this.build(new BuildingMediumData(context.getHandleFor(this), 0, 0));
					SelectionMask mask = null;
					for( AMagLevThing thing : things) {
						thing.draw( built, mask, gc, this);
					}	
				}, new MatTrans());
			}
			isBuilt = true;
			building = false;
		}
	}
	void unbuild() {
		if( this.isBuilt) {
			this.isBuilt = false;
			builtImage.flush();
			builtImage = null;
		}
	}
	
	public void contortBones(BaseBone bone, Interpolator2D state) {
		this.things = BoneContorter.contortBones(this.things, bone, state);
		unbuild();
		Build();
		//context.getHandleFor(this).refresh();
	}
	
	/** Be careful with these things, they can break. */
	public List<AMagLevThing> getThings() {
		return new ArrayList<AMagLevThing>(things);
	}
	

	public class MaglevBuiltData extends ABuiltMediumData {
		MatTrans trans;
		//final int box;
		//final int boy;
		
		public MaglevBuiltData(BuildingMediumData building) 
		{
			super(building.handle);
			this.trans = building.trans;
		}

		@Override public int getWidth() {return context.getWidth(); }
		@Override public int getHeight() {return context.getHeight();}

		@Override public Vec2i convert(Vec2i p) {return p;}
		@Override public Vec2 convert(Vec2 p) {return p;}

		@Override public Rect getBounds() 
		{
			// TODO
			return new Rect( (int)trans.getM02(), (int)trans.getM12(), context.getWidth(), context.getHeight());
		}
		@Override public void draw(GraphicsContext gc) 
		{
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawImage(builtImage.getBase(), builtImage.getXOffset(), builtImage.getYOffset());
			gc.setTransform(oldTrans);
		}
		@Override public void drawBorder(GraphicsContext gc) 
		{
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawRect(0, 0, context.getWidth(), context.getHeight());
			gc.setTransform(oldTrans);
		}
		@Override public MatTrans getCompositeTransform() 
			{return new MatTrans(trans);}
		@Override public MatTrans getScreenToImageTransform() 
		{
			try {
				return trans.createInverse();
			} catch (NoninvertableException e) {
				return new MatTrans();
			}
		}

		// Counter-intuitively, checking in and checking out of a MaglevInternalImage
		//	can be a thing that makes sense to do as the StrokeEngine uses it for the
		//	behavior we want.
		@Override
		protected void _doOnGC(DoerOnGC doer) {
			builtImage.doOnGC(doer, trans);
		}

		@Override
		protected void _doOnRaw(DoerOnRaw doer) {
			builtImage.doOnRaw(doer, trans);
		}
	}


	public AMagLevThing getThingById(int Id) {
		for( AMagLevThing thing : things) {
			if( thing.id == Id)
				return thing;
		}
		return null;
	}
}
