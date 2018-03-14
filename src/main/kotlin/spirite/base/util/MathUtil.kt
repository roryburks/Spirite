package spirite.base.util

import kotlin.math.roundToInt

object MathUtil {
    fun ceil( n: Float) = Math.ceil(n.toDouble()).toInt()

}
val Int.f get() = this.toFloat()
val Int.d get() = this.toDouble()

val Float.floor get() = kotlin.math.floor(this).toInt()
val Float.round get() = this.roundToInt()
val Float.ceil get() = kotlin.math.ceil(this).toInt()

val Double.f get() = this.toFloat()
val Double.floor get() = kotlin.math.floor(this).toInt()
val Double.round get() = this.roundToInt()
val Double.ceil get() = kotlin.math.ceil(this).toInt()