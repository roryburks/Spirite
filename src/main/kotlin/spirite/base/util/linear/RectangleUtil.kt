package spirite.base.util.linear

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import kotlin.math.*

// TODO: Merge into Vectrix once spirite.Rect is replaced with Vectrix.RectI
object RectangleUtil {
    /*
     * Constructs a non-negative dimension RectShape from two coordinates
     */
    fun rectFromEndpoints(x1: Int, y1: Int, x2: Int, y2: Int): Rect {
        return Rect(min(x1, x2), min(y1, y2), abs(x1 - x2), abs(y1 - y2))
    }

    /**
     * Finds the bounds of a rectangle tranformed by a matrix
     */
    fun circumscribeTrans(oldRect: Rect, trans: ITransformF): Rect {
        val (x, y) = trans.apply(Vec2f(oldRect.x.f, oldRect.y.f))
        val (x3, y3) = trans.apply(Vec2f((oldRect.x + oldRect.width).f, oldRect.y.f))
        val (x4, y4) = trans.apply(Vec2f(oldRect.x.f, (oldRect.y + oldRect.height).f))
        val (x5, y5) = trans.apply(Vec2f((oldRect.x + oldRect.width).f, (oldRect.y + oldRect.height).f))

        val x1 = min(floor(x.d), min(floor(x3.d), min(floor(x4.d), floor(x5.d)))).toInt()
        val y1 = min(floor(y.d), min(floor(y3.d), min(floor(y4.d), floor(y5.d)))).toInt()
        val x2 = max(ceil(x.d), max(ceil(x3.d), max(ceil(x4.d), ceil(x5.d)))).toInt()
        val y2 = max(ceil(y.d), max(ceil(y3.d), max(ceil(y4.d), ceil(y5.d)))).toInt()

        return rectFromEndpoints(x1, y1, x2, y2)
    }

    /** Creates the smallest rectangle that contains all given drawPoints.  */
    fun rectFromPoints(points: List<Vec2f>): Rect {
        if (points.isEmpty())
            return Rect(0, 0, 0, 0)
        var x1 = floor(points[0].xf.toDouble()).toInt()
        var y1 = floor(points[0].yf.toDouble()).toInt()
        var x2 = ceil(points[0].xf.toDouble()).toInt()
        var y2 = ceil(points[0].yf.toDouble()).toInt()

        val it = points.iterator()
        it.next()    // Ignore 1 because we already did it
        while (it.hasNext()) {
            val (x, y) = it.next()
            val tx1 = floor(x.toDouble()).toInt()
            val ty1 = floor(y.toDouble()).toInt()
            val tx2 = ceil(x.toDouble()).toInt()
            val ty2 = ceil(y.toDouble()).toInt()
            if (tx1 < x1) x1 = tx1
            if (ty1 < y1) y1 = ty1
            if (tx2 < x2) x2 = tx2
            if (ty2 < y2) y2 = ty2
        }

        return Rect(x1, y1, x2 - x1, y2 - y1)
    }

    /** Stretches the RectShape from the center by a given scaler  */
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
                min(rect1.x, rect2.x),
                min(rect1.y, rect2.y),
                max(rect1.x + rect1.width, rect2.x + rect2.width),
                max(rect1.y + rect1.height, rect2.y + rect2.height)
        )
    }
}