package spirite.base.image_data.mediums.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.mediums.maglev.parts.AMagLevThing;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.interpolation.CubicSplineInterpolatorND;
import spirite.base.util.interpolation.Interpolator2D;

public class BoneContorter {
	public static List<AMagLevThing> contortBones(List<AMagLevThing> source, BaseBone bone, Interpolator2D state) {
		float x1 = bone.x1;
		float y1 = bone.y1;
		float x2 = bone.x2;
		float y2 = bone.y2;
		
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
		
		List<AMagLevThing> myThings = new ArrayList<>(source.size());
		
		for( AMagLevThing thing : source) {
			if( thing instanceof MagLevStroke) {
				PenState[] toTransform = ((MagLevStroke)thing).getPenstates();
				PenState[] transformed = new PenState[toTransform.length];
				if( toTransform == null || toTransform.length == 0) continue;
				
				for( int i=0; i < toTransform.length; ++i) {
					// Step 1: find t, m, points in the rectilinear vector space where
					//	t = 0, m=0 represents x1, y1
					//	t = 1, m=0 represents x2, y2
					//	m represents the distance from the line (left = positive)
					Vec2 a = new Vec2(toTransform[i].x - x1, toTransform[i].y - y1);
					
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
		
		return myThings;
	}
}
