package spirite.base.util

import kotlin.math.roundToInt

object MathUtil {
    fun ceil( n: Float) = Math.ceil(n.toDouble()).toInt()

}
val Int.f get() = this.toFloat()

val Float.floor get() = kotlin.math.floor(this).toInt()
val Float.round get() = this.roundToInt()
val Float.ceil get() = kotlin.math.ceil(this).toInt()