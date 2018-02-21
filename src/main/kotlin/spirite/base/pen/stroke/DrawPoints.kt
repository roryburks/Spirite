package spirite.base.pen.stroke

import spirite.base.util.ApproximateBinarySearch
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

// Made as a class instead of parameters for extendability
open class DrawPoints(val x: FloatArray, val y: FloatArray, val w: FloatArray) {
    val length: Int = x.size

    init {
        if (x.size != y.size || x.size != w.size)
            MDebug.handleWarning(STRUCTURAL, "Miss-matched x/y/w array lengths.")
    }
}

class IndexedDrawPoints(x: FloatArray, y: FloatArray, w: FloatArray, val t: FloatArray) : DrawPoints(x, y, w) {

    init {
        if (x.size != t.size)
            MDebug.handleWarning(STRUCTURAL, "Miss-matched x/y/w/t array lengths.")
    }

    /**
     * @return Returns float in [0,length-1].  Math.round(float) will give the nearest index and
     * Math.floor/Math.ceil can be used for a left or right bound
     */
    fun getNearIndex(met: Float): Float {
        return ApproximateBinarySearch(t, met)
    }
}