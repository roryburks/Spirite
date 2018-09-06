package spirite.base.util.interpolation

import spirite.base.util.MathUtil
import spirite.base.util.linear.Vec2
import java.util.*

/**
 * CubicSplineInterpolator uses Cubic Hermite Spline Interpolation to
 * construct an interpolation function, f(x) made up of piecewise-cubic
 * segments.
 */
class CubicSplineInterpolator
/**
 *
 * @param points_
 * @param fast
 * @param spatial spatial weighting weights the point slopes by
 * the total distance between two points, not just the X-distance.
 * Produces a result very similar (though not identical) to a 2D
 * Cubic Spline that only has points with strictly increasing X values.
 */
(points_: List<Vec2>, fast: Boolean, private val spatial: Boolean) : Interpolator {
    private val k: FloatArray
    private val x_: FloatArray
    private val y_: FloatArray

    val numPoints: Int
        get() = k.size

    init {

        // Sorts the points by X
        val points = ArrayList(points_)

        points.sortWith(Comparator { o1, o2 ->
            val d = o1.x - o2.x
            Math.signum(d).toInt()
        })

        k = FloatArray(points.size)
        x_ = FloatArray(points.size)
        y_ = FloatArray(points.size)

        for (i in points.indices) {
            val p = points[i]
            x_[i] = p.x
            y_[i] = p.y
        }

        fastCalculateSlopes()
    }

    private fun fastCalculateSlopes() {
        if (k.size <= 1) return

        // Note: Enpoint weighting is suppressed a little to avoid wonky
        //	start/end curves

        k[0] = (y_[1] - y_[0]) / (x_[1] - x_[0])

        //var i = 0
        var i = 1
        while (i < k.size - 1) {
            if (spatial) {
                val d1 = MathUtil.distance(x_[i], y_[i], x_[i + 1], y_[i + 1])
                val d2 = MathUtil.distance(x_[i - 1], y_[i - 1], x_[i], y_[i])

                k[i] = ((y_[i + 1] - y_[i]) / d1 + (y_[i] - y_[i - 1]) / d2) / ((x_[i + 1] - x_[i]) / d1 + (x_[i] - x_[i - 1]) / d2)
            } else {

                k[i] = 0.5f * ((y_[i + 1] - y_[i]) / (x_[i + 1] - x_[i]) + (y_[i] - y_[i - 1]) / (x_[i] - x_[i - 1]))
            }
            ++i
        }
        k[i] = (y_[i] - y_[i - 1]) / (x_[i] - x_[i - 1])
    }

    fun getX(n: Int): Float {
        return x_[n]
    }

    fun getY(n: Int): Float {
        return y_[n]
    }

    override fun eval(t: Float): Float {
        if (k.isEmpty()) return 0f


        if (t <= x_[0]) return y_[0]
        if (t >= x_[k.size - 1]) return y_[k.size - 1]

        var i = 0
        while (t > x_[i] && ++i < k.size);
        if (i == k.size) return y_[k.size - 1]


        val dx = x_[i] - x_[i - 1]
        val n = (t - x_[i - 1]) / dx

        val a = k[i - 1] * dx - (y_[i] - y_[i - 1])
        val b = -k[i] * dx + (y_[i] - y_[i - 1])

        return (1 - n) * y_[i - 1] + n * y_[i] + n * (1 - n) * (a * (1 - n) + b * n)
    }

}