package rb.vectrix.shapes

import rb.vectrix.linear.Vec2
import rb.vectrix.linear.Vec2d
import kotlin.math.cos
import kotlin.math.sin


/***
 * A RayRect is a special kind of rectangle designed for detecting collision during movement
 *
 *
 * Restrictions enforced by Make: h >= 0, len >= 0 theta in [0, 2*PI]
 */
abstract class RayRect {
    abstract val x: Double
    abstract val y: Double
    abstract val h: Double
    abstract val len: Double
    abstract val theta: Double

    abstract val top: Double
    abstract val bottom: Double
    abstract val left: Double
    abstract val right: Double

    val lineSegments get() = listOf(back, front, side1, side2)
    abstract val back: LineSegment
    abstract val front: LineSegment
    abstract val side1: LineSegment
    abstract val side2: LineSegment

    abstract val points: List<Vec2>

    abstract val sinth: Double
    abstract val costh: Double

    protected val isinth get() = -sinth
    protected val icosth get() = costh

    protected val m02 by lazy { icosth * (-x) - isinth * (-y) }
    protected val m12 by lazy { isinth * (-x) + icosth * (-y) }

    fun projectX(x: Double, y: Double) = icosth * x - isinth * y + m02
    fun projectY(x: Double, y: Double) = isinth * x + icosth * y + m12

    fun applyDiffX(t: Double, fromEdge: Double = 0.0) = x + costh*(t*len - fromEdge)
    fun applyDiffY(t: Double, fromEdge: Double = 0.0) = y + sinth*(t*len - fromEdge)
    fun travel(len: Double) = Vec2d(x + len*costh, y + len*sinth)

}

data class RayRectD
private constructor(
    override val x: Double,
    override val y: Double,
    override val h: Double,
    override val len: Double,
    override val theta: Double)
    : RayRect()
{
    override val top: Double by lazy { points.map { it.y }.max() ?: 0.0 }
    override val bottom: Double by lazy { points.map { it.y }.min() ?: 0.0 }
    override val left: Double by lazy { points.map { it.x }.min() ?: 0.0 }
    override val right: Double by lazy { points.map { it.x }.max() ?: 0.0 }

    override val back by lazy { LineSegmentD(points[0].x, points[0].y, points[1].x, points[1].y) }
    override val front by lazy { LineSegmentD(points[2].x, points[2].y, points[3].x, points[3].y) }
    override val side1 by lazy { LineSegmentD(points[0].x, points[0].y, points[2].x, points[2].y) }
    override val side2 by lazy { LineSegmentD(points[1].x, points[1].y, points[3].x, points[3].y) }

    override val points by lazy { listOf(
        Vec2d(x - h/2 * sinth, y + h/2 * costh),
        Vec2d(x + h/2 * sinth, y - h/2 * costh),
        Vec2d(x + len * costh - h/2 * sinth, y + len * sinth + h/2 * costh),
        Vec2d(x + len * costh + h/2 * sinth, y + len * sinth - h/2 * costh)) }

    override val sinth: Double by lazy { sin(theta) }
    override val costh: Double by lazy { cos(theta) }

    companion object {
        fun Make( x: Double, y: Double, h: Double, len: Double, theta: Double)
            = RayRectD(x, y, h, len, theta)
    }
}