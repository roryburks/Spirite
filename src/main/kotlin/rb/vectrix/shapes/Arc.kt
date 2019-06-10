package rb.vectrix.shapes

import rb.vectrix.mathUtil.MathUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

abstract class Arc
{
    abstract val x: Double
    abstract val y: Double
    abstract val r: Double
    abstract val angleStart : Double
    abstract val angleEnd : Double

    abstract val xStart: Double
    abstract  val yStart: Double
    abstract val xEnd : Double
    abstract val yEnd : Double

    abstract  val line1 : LineSegment
    abstract val line2 : LineSegment

    inline fun inRange(theta: Double): Boolean {
        val theta2 = MathUtil.cycle(0.0, 2 * PI, theta)
        return if (angleEnd > angleStart) theta2 in angleStart..angleEnd
        else theta2 !in angleStart..angleEnd
    }
}

data class ArcD
private constructor(
    override val x: Double,
    override val y: Double,
    override val r: Double,
    override val angleStart: Double,
    override val angleEnd: Double)
    :Arc()
{
    override val xStart by lazy { x + cos(angleStart) }
    override val yStart by lazy { y + sin(angleStart) }
    override val xEnd by lazy { x + cos(angleEnd) }
    override val yEnd by lazy { y + sin(angleEnd) }

    override val line1 get() = LineSegmentD(x, y, xStart, yStart)
    override val line2 get() = LineSegmentD(x,y,xEnd, yEnd)

    companion object {
        fun Make(x: Double, y: Double, r: Double, angleStart: Double, angleEnd: Double) =
            ArcD(x, y, r,
                MathUtil.cycle(0.0, 2* PI, angleStart),
                MathUtil.cycle(0.0, 2* PI, angleEnd))
    }
}