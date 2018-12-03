package spirite.base.util

import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import rb.vectrix.linear.Vec2f
import spirite.base.util.RectangleUtil.rectFromEndpoints
import kotlin.math.roundToInt

object MathUtil {
    // ==============
    // ==== Math Functions
    fun packInt(high: Int, low: Int): Int {
        return high and 0xffff shl 16 or (low and 0xffff)
    }

    fun low16(i: Int): Int {
        return i and 0xffff
    }

    fun high16(i: Int): Int {
        return i.ushr(16)
    }

    fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    }

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble()).toFloat()
    }

    fun clip(min: Int, value: Int, max: Int): Int {
        if (value < min) return min
        return if (value > max) max else value
    }

    fun clip(min: Double, value: Double, max: Double): Double {
        if (value < min) return min
        return if (value > max) max else value
    }

    fun clip(min: Float, value: Float, max: Float): Float {
        if (value < min) return min
        return if (value > max) max else value
    }

    fun lerp(a: Float, b: Float, t: Float): Float {
        return t * b + (1 - t) * a
    }

    fun lerp(a: Double, b: Double, t: Double): Double {
        return t * b + (1 - t) * a
    }

    fun cycle( start: Int, end: Int, t: Int) = when( val diff = end - start){
        0 -> 0
        else -> ((t - start) % diff + diff) % diff + start
    }
    fun cycle(start: Float, end: Float, t: Float) = when( val diff = end - start){
        0.0f -> 0.0f
        else -> ((t - start) % diff + diff) % diff + start
    }
    fun cycle(start: Double, end: Double, t: Double): Double = when( val diff = end - start) {
        0.0 -> 0.0
        else -> ((t - start) % diff + diff) % diff + start
    }

    /**point into two coordinates: the first representing
    * its projection onto the line segment normalized such that t=0 means it's perpendicular
    * to (x1,y1) and t=1 for (x2,y2).  The second representing the distance from the line
    * extended from the line segment
    */
    fun projectOnto(x1: Float, y1: Float, x2: Float, y2: Float, p: Vec2f): Vec2f {
        val b = Vec2f(x2 - x1, y2 - y1)
        val scale_b = b.mag
        val scale_b2 = scale_b * scale_b

        val a = Vec2f(p.xf - x1, p.yf - y1)

        val t = a.dot(b) / scale_b2    // the extra / ||b|| is to normalize it to ||b|| = 1
        val m = a.cross(b) / scale_b

        return Vec2f(t, m)
    }

}

object RectangleUtil {
    /**
     * Constructs a non-negative dimension Rectangle from two coordinates
     */
    fun rectFromEndpoints(x1: Int, y1: Int, x2: Int, y2: Int): Rect {
        return Rect(Math.min(x1, x2), Math.min(y1, y2),
                Math.abs(x1 - x2), Math.abs(y1 - y2))
    }

    /**
     * Finds the bounds of a rectangle tranformed by a matrix
     */
    fun circumscribeTrans(oldRect: Rect, trans: Transform): Rect {
        val (x, y) = trans.apply(Vec2f(oldRect.x.toFloat(), oldRect.y.toFloat()))
        val (x3, y3) = trans.apply(Vec2f((oldRect.x + oldRect.width).toFloat(), oldRect.y.toFloat()))
        val (x4, y4) = trans.apply(Vec2f(oldRect.x.toFloat(), (oldRect.y + oldRect.height).toFloat()))
        val (x5, y5) = trans.apply(Vec2f((oldRect.x + oldRect.width).toFloat(), (oldRect.y + oldRect.height).toFloat()))

        val x1 = Math.min(Math.floor(x.toDouble()), Math.min(Math.floor(x3.toDouble()), Math.min(Math.floor(x4.toDouble()), Math.floor(x5.toDouble())))).toInt()
        val y1 = Math.min(Math.floor(y.toDouble()), Math.min(Math.floor(y3.toDouble()), Math.min(Math.floor(y4.toDouble()), Math.floor(y5.toDouble())))).toInt()
        val x2 = Math.max(Math.ceil(x.toDouble()), Math.max(Math.ceil(x3.toDouble()), Math.max(Math.ceil(x4.toDouble()), Math.ceil(x5.toDouble())))).toInt()
        val y2 = Math.max(Math.ceil(y.toDouble()), Math.max(Math.ceil(y3.toDouble()), Math.max(Math.ceil(y4.toDouble()), Math.ceil(y5.toDouble())))).toInt()

        return rectFromEndpoints(x1, y1, x2, y2)
    }

    /** Creates the smallest rectangle that contains all given points.  */
    fun rectFromPoints(points: List<Vec2f>): Rect {
        if (points.isEmpty())
            return Rect(0, 0, 0, 0)
        var x1 = Math.floor(points[0].xf.toDouble()).toInt()
        var y1 = Math.floor(points[0].yf.toDouble()).toInt()
        var x2 = Math.ceil(points[0].xf.toDouble()).toInt()
        var y2 = Math.ceil(points[0].yf.toDouble()).toInt()

        val it = points.iterator()
        it.next()    // Ignore 1 because we already did it
        while (it.hasNext()) {
            val (x, y) = it.next()
            val tx1 = Math.floor(x.toDouble()).toInt()
            val ty1 = Math.floor(y.toDouble()).toInt()
            val tx2 = Math.ceil(x.toDouble()).toInt()
            val ty2 = Math.ceil(y.toDouble()).toInt()
            if (tx1 < x1) x1 = tx1
            if (ty1 < y1) y1 = ty1
            if (tx2 < x2) x2 = tx2
            if (ty2 < y2) y2 = ty2
        }

        return Rect(x1, y1, x2 - x1, y2 - y1)
    }

    /** Stretches the Rectangle from the center by a given scaler  */
    fun scaleRect(cropSection: Rect, scalar: Float): Rect {
        return Rect(
                cropSection.x - Math.round(cropSection.width * (scalar - 1) / 2.0f),
                cropSection.y - Math.round(cropSection.height * (scalar - 1) / 2.0f),
                Math.round(cropSection.width * scalar),
                Math.round(cropSection.height * scalar)
        )

    }

    /** Returns the smallest rectangle such that rect1 and rect2 are contained
     * within it.	  */
    fun circumscribe(rect1: Rect, rect2: Rect): Rect {
        return rectFromEndpoints(
                Math.min(rect1.x, rect2.x),
                Math.min(rect1.y, rect2.y),
                Math.max(rect1.x + rect1.width, rect2.x + rect2.width),
                Math.max(rect1.y + rect1.height, rect2.y + rect2.height)
        )
    }
}