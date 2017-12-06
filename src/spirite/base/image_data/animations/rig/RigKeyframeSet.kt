package spirite.base.image_data.animations.rig

import spirite.base.util.interpolation.CubicSplineInterpolatorND
import java.util.*

class RigKeyframeSet {
    private val frameMap = TreeMap<Float, RigKeyframe>()
    val keyFrames : Map<Float,RigKeyframe> get() {return frameMap.toMap()}


    private var _interpolator: CubicSplineInterpolatorND? = null
    val interpolator : CubicSplineInterpolatorND? get() {
        val int = _interpolator
        if( int == null) {
            if( frameMap.isEmpty())
                return null

            val data = Array(frameMap.size, {FloatArray(6)})
            val sorted = frameMap.keys.sorted()

            for( i in 0 until frameMap.size) {
                val key = frameMap[sorted[i]]!!
                data[i][0] = key.tx
                data[i][1] = key.ty
                data[i][2] = key.sx
                data[i][3] = key.sy
                data[i][4] = key.rot
                data[i][5] = sorted[i]
            }

            val int2 = CubicSplineInterpolatorND(data, 5, frameMap.size, false)
            _interpolator = int2
            return int2
        }
        return int
    }

    fun addKeyframe( t: Float, rkf: RigKeyframe) {
        frameMap.put(t, rkf)
        _interpolator = null
    }
    fun removeKeyframe( t: Float) {
        frameMap.remove(t)
        _interpolator = null
    }
    fun removeKeyframeInRange( start: Float?, end: Float?) {
        frameMap.entries.removeIf {(start==null || it.key >= start) && (end==null || it.key <= end)}
        _interpolator = null
    }
    fun clearKeyFrames() {
        frameMap.clear()
        _interpolator = null
    }

    fun getFrameAtT( t:Float) : RigKeyframe {
        val datum = interpolator?.eval(t) ?: return RigKeyframe()
        return RigKeyframe(datum[0], datum[1], datum[2], datum[3], datum[4])
    }
}