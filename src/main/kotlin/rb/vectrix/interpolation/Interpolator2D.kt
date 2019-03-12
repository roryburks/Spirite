package rb.vectrix.interpolation

import rb.vectrix.linear.Vec2f

interface Interpolator2D {

    val curveLength: Float

    data class InterpolatedPoint(
            val x: Float,
            val y: Float,
            val lerp: Float,
            val left: Int,
            val right: Int)

    fun addPoint(x: Float, y: Float)
    fun eval(t: Float): Vec2f
    fun evalExt(t: Float): Interpolator2D.InterpolatedPoint
}