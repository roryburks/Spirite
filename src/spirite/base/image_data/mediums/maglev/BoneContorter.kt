package spirite.base.image_data.mediums.maglev

import spirite.base.util.interpolation.Interpolator2D
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone
import spirite.base.util.linear.Vec2
import spirite.base.util.interpolation.CubicSplineInterpolatorND
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke
import spirite.base.pen.PenTraits.PenState
import java.util.*

fun contortBones( source:List<AMagLevThing>, bone:BaseBone, state:Interpolator2D): List<AMagLevThing> {
	val x1 = bone.x1
	val y1 = bone.y1
	val x2 = bone.x2
	val y2 = bone.y2
	
	val b = Vec2(x2-x1, y2-y1)
	val scaleB = b.mag
	val scaleB2 = scaleB*scaleB
	val cLen = state.curveLength
	
	// Construct normal Interpolator
	val normals = CubicSplineInterpolatorND(arrayOf<FloatArray>(), 2, 0, true)
	var from = state.eval(0f)
	
    var s = 1f
    while (s < cLen + 1)
    {
      val to = state.eval(s)
      var normal = to.sub(from)
      normal = (Vec2(-normal.y, normal.x)).normalize()	// Rotate 90 degrees, then normalize
      normals.addPoint(floatArrayOf(normal.x, normal.y, (s - 1) / cLen))
      from = to
      s += 1f
    }
	// TODO: Soften normals such that the rate of change never produces a circle
	//	smaller than the radius of the width determined in the bone's weightmap
	//	(possibly implementing some sort of "pinching"

	
	val myThings = ArrayList<AMagLevThing>(source.size)
	
	
	for( thing in source) {
		if ( thing is MagLevStroke){
			val toTransform = thing.penstates
			if( toTransform?.size ?: 0 == 0)
				continue
			
			val transformed = toTransform.map { 
				// Step 1: find t, m, points in the rectilinear vector space where
				//	t = 0, m=0 represents x1, y1
				//	t = 1, m=0 represents x2, y2
				//	m represents the distance from the line (left = positive)
				val a = Vec2(it.x - x1, it.y - y1)
				val t = a.dot(b) / scaleB2		// the extra / ||b|| is to normalize it to ||b|| = 1
				val m = a.cross(b) / scaleB
				
				// Step 2: Use the t to calculate the point on the interpolator to
				//	branch off of and use m as the distance to branch off of it
				val p = state.eval( t*cLen)
				val normal = normals.eval(t)
				
				PenState(p.x - normal[0]*m, p.y-normal[1]*m, it.pressure)
			}
			
			myThings.add( MagLevStroke( transformed.toTypedArray(), thing.params, thing.id))
		}
		else myThings.add( thing)
	}
	
	return myThings
}