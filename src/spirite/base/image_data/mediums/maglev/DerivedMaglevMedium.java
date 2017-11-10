package spirite.base.image_data.mediums.maglev;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.layers.puppet.BasePuppet;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.interpolation.Interpolator2D;

/**
 * A DerivedMaglevMedium is a second-hand medium that relies on a MaglevMedium.
 * PuppetLayers use these to create complexly-transformed variations of MaglevMediums
 * without destroying data.  For the most part it is a bone-transformed variation
 * of the MaglevMedium, but it can have additional stroke and fill components for
 * touch-ups, e.g. if bending the bone a certain way exposes some seams in the geometry.
 */
public class DerivedMaglevMedium 
	implements IMedium
{
	public final MaglevMedium derivedFrom;
	
	public DerivedMaglevMedium( MaglevMedium derivedFrom) {
		this.derivedFrom = derivedFrom;
	}
	
	public DerivedMaglevMedium(DerivedMaglevMedium other) {
		this.derivedFrom = other.derivedFrom;
	}

	public void deriveFromBone( BasePuppet.BaseBone bone, Interpolator2D state) {
		
	}
	
	public boolean passThrough() {
		return true;
	}

	// TODO
	@Override public int getWidth() {return (passThrough() ? derivedFrom.getWidth() : 0);}
	@Override public int getHeight() {return (passThrough() ? derivedFrom.getHeight() : 0);}
	@Override public int getDynamicX() {return (passThrough() ? derivedFrom.getDynamicX() : 0);}
	@Override public int getDynamicY() { return (passThrough() ? derivedFrom.getDynamicY() : 0);}

	@Override
	public ABuiltMediumData build(BuildingMediumData building) {
		return new DerivedMaglevBuiltData(derivedFrom.build(building));
	}

	@Override public IMedium dupe() {return new DerivedMaglevMedium(this);}
	@Override
	public IMedium copyForSaving() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
	}

	@Override
	public IImage readOnlyAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override public InternalImageTypes getType() {return InternalImageTypes.DERIVED_MAGLEV;}

	@Override
	public IImageDrawer getImageDrawer(BuildingMediumData building) {
		// TODO Auto-generated method stub
		return null;
	}

	
	class DerivedMaglevBuiltData extends ABuiltMediumData {
		ABuiltMediumData base;

		public DerivedMaglevBuiltData(ABuiltMediumData base) {
			super(base.handle);
			this.base = base;
		}

		@Override public int getWidth() {return base.getWidth();}
		@Override public int getHeight() {return base.getHeight();}
		@Override
		public void draw(GraphicsContext gc) {
			base.draw(gc);
		}

		@Override
		public void drawBorder(GraphicsContext gc) {
			base.drawBorder(gc);
		}

		@Override protected void _doOnGC(DoerOnGC doer) {}
		@Override protected void _doOnRaw(DoerOnRaw doer) {}

		@Override
		public MatTrans getScreenToImageTransform() {
			return base.getScreenToImageTransform();
		}

		@Override
		public Vec2i convert(Vec2i p) {
			return base.convert(p);
		}

		@Override
		public Vec2 convert(Vec2 p) {
			return base.convert(p);
		}

		@Override
		public Rect getBounds() {
			return base.getBounds();
		}

		@Override
		public MatTrans getCompositeTransform() {
			return base.getCompositeTransform();
		}
		
	}
}
