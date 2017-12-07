package spirite.base.pen

import spirite.base.util.ApproximateBinarySearch

class IndexedDrawPoints(x: FloatArray, y: FloatArray, w: FloatArray, val t: FloatArray) : DrawPoints(x, y, w) {
    /**
     * @return Returns float in [0,length-1].  Math.round(float) will give the nearest index and
     * Math.floor/Math.ceil can be used for a left or right bound
     */
    fun getNearIndex(met: Float): Float {
        return ApproximateBinarySearch(t, met)
    }
}