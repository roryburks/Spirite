package rb.vectrix.interpolation

import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import kotlin.math.sign

/**
 * CubicSplineInterpolator uses Cubic Hermite Spline Interpolation to
 * construct an interpolation function, f(xi) made up of piecewise-cubic
 * segments.
 */
class CubicSplineInterpolator
/**
 *
 * @param points_
 * @param spatial spatial weighting weights the point slopes by
 * the total distance between two drawPoints, not just the AnimationCommand-distance.
 * Produces a result very similar (though not identical) to a 2D
 * Cubic Spline that only has drawPoints with strictly increasing AnimationCommand values.
 */
(points_: List<Vec2f>, private val spatial: Boolean) : Interpolator {
    private val k: FloatArray
    private val x_: FloatArray
    private val y_: FloatArray

    val numPoints: Int
        get() = k.size

    init {

        // Sorts the drawPoints by AnimationCommand
        val points = ArrayList(points_)

        points.sortWith(Comparator { o1, o2 ->
            val d = o1.xf - o2.xf
            sign(d).toInt()
        })

        k = FloatArray(points.size)
        x_ = FloatArray(points.size)
        y_ = FloatArray(points.size)

        for (i in points.indices) {
            val p = points[i]
            x_[i] = p.xf
            y_[i] = p.yf
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