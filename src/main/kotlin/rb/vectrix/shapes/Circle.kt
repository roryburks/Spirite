package rb.vectrix.shapes

import kotlin.math.abs

abstract class Circle
{
    abstract val x : Double
    abstract val y : Double
    abstract val r : Double
}


data class CircleD
private constructor(
    override val x: Double,
    override val y: Double,
    override val r: Double)
    : Circle()
{
    companion object {
        fun Make(x: Double, y: Double, r:Double) = CircleD(x, y, abs(r))
    }
}