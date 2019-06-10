package rb.vectrix.shapes

import rb.vectrix.linear.Vec2
import rb.vectrix.linear.Vec2d
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
import kotlin.math.max
import kotlin.math.min

abstract class LineSegment
{
    abstract val x1: Double
    abstract val y1: Double
    abstract val x2: Double
    abstract val y2: Double

    abstract val left: Double
    abstract val right: Double
    abstract val top: Double
    abstract val bottom: Double

    abstract val dist: Double
    abstract val normal : Vec2
    abstract val m : Double
    abstract val b : Double

    fun lerp(t: Double) = Vec2d(x1 + (x2 - x1)*t, y1 + (y2 - y1) * t)

    infix fun intersection( other: LineSegment) : Double?
    {
        val denom = (other.x2 - other.x1) * (y1 - y2) - (x1 - x2) * (other.y2 - other.y1)
        if( denom == 0.0) return null

        val a = (other.y1 - other.y2)*(x1 - other.x1) + (other.x2 - other.x1)*(y1 - other.y1)
        val t = a/denom


        if( t < 0) return null
        if( t > 1) return null

        val b = (y1 - y2)*(x1-other.x1) + (x2 - x1) *  (y1 - other.y1)
        val t1 = b/denom
        if( t1 < 0) return null
        if( t1 > 1) return null

        return t
    }
}

data class LineSegmentD(
    override val x1: Double, override val y1: Double, override val x2: Double, override val y2: Double)
    : LineSegment()
{
    override val left get() = min(x1, x2)
    override val right get() = max(x1, x2)
    override val top get() = max(y1,y2)
    override val bottom get() = min(y1,y2)

    override val dist by lazy { MathUtil.distance(x1,y1, x2, y2) }
    override val normal by lazy {
        if(dist == 0.0) Vec2d(1.0, 0.0)
        else Vec2d((y2 - y1) / dist, -(x2 - x1) / dist)
    }

    override val m: Double get() = (y2-y1) / (x2-x1)
    override val b: Double get() = y1 - m * x1

    override fun toString() = "($x1, $y1) - ($x2, $y2)"
}

data class LineSegmentI(
    val x1i: Int,
    val y1i: Int,
    val x2i: Int,
    val y2i: Int)
    :LineSegment()
{
    override val x1: Double get() = x1i.d
    override val y1: Double get() = y1i.d
    override val x2: Double get() = x2i.d
    override val y2: Double get() = y2i.d

    val lefti get() = min(x1i, x2i)
    val righti get() = max(x1i, x2i)
    val topi get() = max(y1i, y2i)
    val bottomi get() = min(y1i, y2i)
    override val left: Double get() = lefti.d
    override val right: Double get() = righti.d
    override val top: Double get() = topi.d
    override val bottom: Double get() = bottomi.d

    override val dist: Double by lazy { MathUtil.distance(x1,y1, x2, y2) }
    override val normal: Vec2  by lazy {
        if(dist == 0.0) Vec2d(1.0, 0.0)
        else Vec2d((y2 - y1) / dist, -(x2 - x1) / dist)
    }

    override val m: Double get() = (y2-y1) / (x2-x1)
    override val b: Double get() = y1 - m * x1
}