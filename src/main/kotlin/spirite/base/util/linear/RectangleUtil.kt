package spirite.base.util.linear

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Vec2f

// TODO: Merge into Vectrix once spirite.Rect is replaced with Vectrix.RectI
object RectangleUtil {
    /**
     * Constructs a non-negative dimension RectShape from two coordinates
     */
    fun rectFromEndpoints(x1: Int, y1: Int, x2: Int, y2: Int): Rect {
        return Rect(Math.min(x1, x2), Math.min(y1, y2),
                Math.abs(x1 - x2), Math.abs(y1 - y2))
    }

    /**
     * Finds the bounds of a rectangle tranformed by a matrix
     */
    fun circumscribeTrans(oldRect: Rect, trans: ITransformF): Rect {
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

    /** Creates the smallest rectangle that contains all given drawPoints.  */
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
                Math.min(rect1.x, rect2.x),
                Math.min(rect1.y, rect2.y),
                Math.max(rect1.x + rect1.width, rect2.x + rect2.width),
                Math.max(rect1.y + rect1.height, rect2.y + rect2.height)
        )
    }
}