package spirite.base.util

object MathUtil {
    fun ceil( n: Float) = Math.ceil(n.toDouble()).toInt()

}
val Int.f get() = this.toFloat()