package spirite.base.image_data.mediums.maglev;

import org.jetbrains.annotations.NotNull;
import spirite.base.graphics.DynamicImage;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.layers.puppet.BasePuppet;
import spirite.base.image_data.mediums.*;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.interpolation.Interpolator2D;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;

import java.util.ArrayList;

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
	
	private boolean flat = true;
	private final ArrayList<AMagLevThing> extraThings;
	private boolean built;
	private BasePuppet.BaseBone baseBone;
	private DynamicImage rendered;
	
	public DerivedMaglevMedium( MaglevMedium derivedFrom) {
		this.derivedFrom = derivedFrom;
		extraThings = new ArrayList<>();
	}
	
	public DerivedMaglevMedium(DerivedMaglevMedium other) {
		this.derivedFrom = other.derivedFrom;
		this.extraThings = new ArrayList<>(other.extraThings);
	}

	public void deriveFromBone( BasePuppet.BaseBone bone, Interpolator2D state) {
		// TODO
		
	}
//	public void boneConform( float x1, float y1, float x2, float y2, Interpolator2D to) {
//	Vec2 b = new Vec2(x2-x1, y2-y1);
//	float scale_b = b.getMag();
//	float scale_b_s = scale_b*scale_b;
//	
//	for( MagLevThing thing : things) {
//		float[] toTransform = thing.getPoints();
//		if( toTransform == null) continue;
//		
//		for( int i=0; i < toTransform.length; i += 2) {
//			Vec2 a = new Vec2(toTransform[i]-x1, toTransform[i+1]-y1);
//			
//			float scale_a = a.getMag();
//			float a1 = a.getMag();
//			Vec2 a2 = a.sub(b.scalar(a.dot(b)/b.dot(b)));
//		}
//		
//	}
//}
	
	public boolean passThrough() {return flat;}

	@Override public int getWidth() {return (passThrough() ? derivedFrom.getWidth() : (built ? rendered.getWidth() : 0));}
	@Override public int getHeight() {return (passThrough() ? derivedFrom.getHeight() : (built ? rendered.getHeight() : 0));}
	@Override public int getDynamicX() {return (passThrough() ? derivedFrom.getDynamicX() : (built ? rendered.getXOffset(): 0));}
	@Override public int getDynamicY() { return (passThrough() ? derivedFrom.getDynamicY() : (built ? rendered.getYOffset() : 0));}

	@Override
	public BuiltMediumData build(BuildingMediumData building) {
		return new DerivedMaglevBuiltData(derivedFrom.build(building), building);
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

	
	class DerivedMaglevBuiltData extends BuiltMediumData {
		BuiltMediumData base;

		public DerivedMaglevBuiltData(BuiltMediumData base, BuildingMediumData building) {
			super(building);
			this.base = base;
		}

		@NotNull
		@Override
		public MatTrans getDrawTrans() {return base.getDrawTrans();}

		@Override
		public int getDrawWidth() {
			return base.getDrawWidth();
		}

		@Override
		public int getDrawHeight() {
			return base.getDrawHeight();
		}

		@NotNull
		@Override
		public MatTrans getSourceTransform() {
			return base.getSourceTransform();
		}

		@Override
		public int getSourceWidth() {
			return base.getSourceWidth();
		}

		@Override
		public int getSourceHeight() {
			return base.getSourceHeight();
		}

		@Override
		protected void _doOnGC(@NotNull DoerOnGC doer) {}

		@Override
		protected void _doOnRaw(@NotNull DoerOnRaw doer) {}

		/*
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
		}*/
		
	}
}
