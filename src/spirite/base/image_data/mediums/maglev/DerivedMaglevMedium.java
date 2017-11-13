package spirite.base.image_data.mediums.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.DynamicImage;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.layers.puppet.BasePuppet;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.maglev.parts.AMagLevThing;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;
import spirite.base.util.interpolation.CubicSplineInterpolatorND;
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
		this.baseBone = bone;
		
		int x1 = bone.x1;
		int y1 = bone.y1;
		int x2 = bone.x2;
		int y2 = bone.y2;
		
		Vec2 b = new Vec2(x2 - x1, y2 - y1);
		float scale_b = b.getMag();
		float scale_b2 = scale_b*scale_b;
		float clen = state.getCurveLength();
		
		// Construct normal Interpolator
		CubicSplineInterpolatorND normals = new CubicSplineInterpolatorND(new float[][] {}, 2, 0, true);
		Vec2 from = state.eval(0);
		for( float s = 1; s < clen+1; s += 1) {
			Vec2 to = state.eval(s);
			Vec2 normal = to.sub(from);
			normal = (new Vec2(-normal.y, normal.x)).normalize();	// Rotate 90 degrees, then normalize
			normals.addPoint( new float[] { normal.x, normal.y, (s-1)/clen});
			from = to;
		}
		
		// TODO: Soften normals such that the rate of change never produces a circle
		//	smaller than the radius of the width determined in the bone's weightmap
		//	(possibly implementing some sort of "pinching"
		
		List<AMagLevThing> myThings = new ArrayList<>(derivedFrom.things.size());
		
		for( AMagLevThing thing : derivedFrom.things) {
			if( thing instanceof MagLevStroke) {
				PenState[] toTransform = ((MagLevStroke)thing).getPenstates();
				PenState[] transformed = new PenState[toTransform.length];
				if( toTransform == null || toTransform.length == 0) continue;
				
				for( int i=0; i < toTransform.length; ++i) {
					// Step 1: find t, m, points in the rectilinear vector space where
					//	t = 0, m=0 represents x1, y1
					//	t = 1, m=0 represents x2, y2
					//	m represents the distance from the line (left = positive)
					Vec2 a = new Vec2(toTransform[i].x - x1, toTransform[i+1].y - y1);
					
					float t =  a.dot(b) / scale_b2;	// the extra / ||b|| is to normalize it to ||b|| = 1
					float m = a.cross(b) / scale_b;
					
					// Step 2: Use the t to calculate the point on the interpolator to 
					//	branch off of and use m as the distance to branch off of it
					Vec2 p = state.eval(t * clen);
					float[] normal = normals.eval(t);
					transformed[i] = new PenState(
							p.x + normal[0]*m, 
							p.y + normal[1]*m, 
							toTransform[i].pressure);
				}
				
				myThings.add(new MagLevStroke(transformed, ((MagLevStroke)thing).params));
			}
			else 
				myThings.add(thing);
				
		}
		
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
