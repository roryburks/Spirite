package spirite.base.pen.stroke

import rb.vectrix.calculate.nearestBinarySearch
import sguiSwing.hybrid.MDebug
import sguiSwing.hybrid.MDebug.WarningType.STRUCTURAL

// Made as a class instead of parameters for extendability
open class DrawPoints(val x: FloatArray, val y: FloatArray, val w: FloatArray) {
    val length: Int = x.size

    init {
        if (x.size != y.size || x.size != w.size)
            MDebug.handleWarning(STRUCTURAL, "Miss-matched xi/yi/wf array lengths.")
    }

    open fun dupe() = DrawPoints(x.clone(), y.clone(), w.clone())
}

class IndexedDrawPoints(x: FloatArray, y: FloatArray, w: FloatArray, val t: FloatArray) : DrawPoints(x, y, w) {

    init {
        if (x.size != t.size)
            MDebug.handleWarning(STRUCTURAL, "Miss-matched xi/yi/wf/t array lengths.")
    }

    /**
     * @return Returns float in [0,length-1].  Math.round(float) will give the nearest index and
     * Math.floor/Math.ceil can be used for a left or right bound
     */
    fun getNearIndex(met: Float): Float {
        return nearestBinarySearch(t, met)
    }

    override fun dupe() = IndexedDrawPoints(x.clone(), y.clone(), w.clone(), t.clone())
}