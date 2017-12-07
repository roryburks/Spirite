package spirite.base.pen

import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

// Made as a class instead of parameters for extendability
open class DrawPoints(val x: FloatArray, val y: FloatArray, val w: FloatArray) {
    val length: Int

    init {

        this.length = x.size
        if (x.size != y.size || x.size != w.size)
            MDebug.handleWarning(STRUCTURAL, "Miss-matched x/y array lengths.")
    }
}