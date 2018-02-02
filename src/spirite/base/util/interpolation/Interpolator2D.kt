package spirite.base.util.interpolation

import spirite.base.util.linear.Vec2

interface Interpolator2D {

    val curveLength: Float

    data class InterpolatedPoint(
            val x: Float,
            val y: Float,
            val lerp: Float,
            val left: Int,
            val right: Int)

    fun addPoint(x: Float, y: Float)
    fun eval(t: Float): Vec2
    fun evalExt(t: Float): Interpolator2D.InterpolatedPoint
}