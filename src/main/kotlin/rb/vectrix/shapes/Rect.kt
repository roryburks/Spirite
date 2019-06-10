package rb.vectrix.shapes

import rb.vectrix.linear.Vec2
import rb.vectrix.linear.Vec2d
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.d
import kotlin.math.abs
import kotlin.math.min

abstract class Rect
{
    abstract val x1 : Double
    abstract val y1 : Double
    abstract val x2 : Double
    abstract val y2 : Double
    abstract val w : Double
    abstract val h : Double

    abstract val points : List<Vec2>
    abstract val lineSegments : List<LineSegment>

    infix fun intersects(r : Rect) : Boolean {
        var tw = this.w
        var th = this.h
        var rw = r.w
        var rh = r.h
        if (rw < 0 || rh < 0 || tw < 0 || th < 0) {
            return false
        }
        val tx = this.x1
        val ty = this.y1
        val rx = r.x1
        val ry = r.y1
        rw += rx
        rh += ry
        tw += tx
        th += ty
        //      overflow || intersect
        return (rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry)
    }

    fun contains(x: Double, y: Double) =
            if (w <= 0 || h <= 0) false
            else !(x2 < x || y2 < y || x2 > x + w || y2 > y + h)
}



data class RectD(
        override val x1: Double,
        override val y1: Double,
        override val w: Double,
        override val h: Double)
    : Rect()
{
    override val x2: Double get() = x1 + w
    override val y2: Double get() = y1 + h

    override val points: List<Vec2> get() = listOf(Vec2d(x1, y1), Vec2d(x2, y1), Vec2d(x1, y2), Vec2d(x2, y2))
    override val lineSegments: List<LineSegment> get() = listOf(
            LineSegmentD(x1, y1, x2, y1),
            LineSegmentD(x2, y1, x2, y2),
            LineSegmentD(x1, y2, x2, y2),
            LineSegmentD(x1, y1, x1, y2))

    companion object {
        fun FromEndpoints( x1: Double, y1: Double, x2: Double, y2: Double)
                = RectD(min(x1, x2), min(y1,y2), abs(x1-x2), abs(y2-y1))

        fun FromPoints( xs : Sequence<Double>, ys: Sequence<Double>) : RectD
        {
            val x1 = xs.min() ?: 0.0
            val y1 = ys.min() ?: 0.0
            val x2 = xs.max() ?: 0.0
            val y2 = ys.max() ?: 0.0
            return RectD(x1, y1, x2-x1, y2-y1)
        }
        fun FromPoints( points : Sequence<Vec2>) = FromPoints(points.map { it.x }, points.map { it.y })
    }
}

data class RectI(
        val x1i: Int,
        val y1i: Int,
        val wi: Int,
        val hi: Int)
    :Rect()
{
    override val x1: Double get() = x1i.d
    override val y1: Double get() = y1i.d
    override val w: Double get() = wi.d
    override val h: Double get() = hi.d

    val x2i get() = x1i + wi
    val y2i get() = y1i + hi

    override val x2: Double get() = x2i.d
    override val y2: Double get() = y2i.d

    override val points: List<Vec2i> get() = listOf(Vec2i(x1i, y1i), Vec2i(x2i, y1i), Vec2i(x1i, y2i), Vec2i(x2i, y2i))
    override val lineSegments: List<LineSegmentI> get() = listOf(
            LineSegmentI(x1i, y1i, x2i, y1i),
            LineSegmentI(x2i, y1i, x2i, y2i),
            LineSegmentI(x1i, y2i, x2i, y2i),
            LineSegmentI(x1i, y1i, x2i, y2i))

    fun contains(x: Int, y: Int) =
            if (w <= 0 || h <= 0) false
            else !(x2 < x || y2 < y || x2 > x + w || y2 > y + h)
}