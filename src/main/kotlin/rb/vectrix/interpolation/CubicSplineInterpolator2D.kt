package rb.vectrix.interpolation

import rb.vectrix.VectrixMathLayer
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import kotlin.math.max
import kotlin.math.min

/**
 * CubicSplineInterpolator2D is a two-dimensional curve interpolator which
 * uses Cubic Hermite Spline interpolation from a given number of drawPoints
 * to interpolate a 2D curve.  It maintains two separate Hermite Splines:
 * one for the AnimationCommand-axis, and another for the Y-axis, which are both traversed
 * along a common axis (t).
 *
 * The range of t is determined by the length of the line segments which
 * make up the key-drawPoints.
 */
class CubicSplineInterpolator2D : Interpolator2D {
    private var kx: FloatArray    // xi-differentials
    private var ky: FloatArray    // yi-differentials
    private var x_: FloatArray    // xi-values
    private var y_: FloatArray    // yi-values
    private var t_: FloatArray    // t-values
    var numPoints = 0
        private set
    private var distance = 0f

    private val fast: Boolean
    var isExtrapolating = false

    /**
     *
     * @param points
     * @param fast
     *  * If true, use Finite difference to get the slope vectors
     * (i.e. takes the average of the two distance vectors to/from the drawPoints).
     *  * If false, it finds the slopes such that the second-degree derivative
     * of the curve is 0 at each point (by solving the tridiagonal linear equation
     * system)
     * !!!!CURRENTLY UNIMPLEMENTED!!!!
     */
    constructor(points: List<Vec2f>? = null, fast: Boolean = false) {
        numPoints = points?.size ?: 0

        val l = max(numPoints, 10)
        kx = FloatArray(l)
        ky = FloatArray(l)
        x_ = FloatArray(l)
        y_ = FloatArray(l)
        t_ = FloatArray(l)
        distance = 0f

        if (numPoints != 0) {
            x_[0] = points!![0].xf
            y_[0] = points[0].yf
            t_[0] = 0f

        }

        for (i in 1 until numPoints) {
            x_[i] = points!![i].xf
            y_[i] = points[i].yf
            t_[i] = distance + MathUtil.distance(x_[i - 1], y_[i - 1], x_[i], y_[i])
            distance = t_[i]
        }

        this.fast = fast

        //            if( !fast)
        //                MDebug.handleWarning(MDebug.WarningType.UNSUPPORTED, null, "Precise smoothing not implemented.");

        fastCalculateSlopes()
    }

    constructor(xs: FloatArray?, ys: FloatArray?, fast: Boolean) {
        numPoints = min(xs?.size ?: 0, ys?.size ?: 0)

        val l = max(numPoints, 10)
        kx = FloatArray(l)
        ky = FloatArray(l)
        x_ = FloatArray(l)
        y_ = FloatArray(l)
        t_ = FloatArray(l)
        distance = 0f

        if (numPoints != 0) {
            x_[0] = xs!![0]
            y_[0] = ys!![0]
            t_[0] = 0f

        }

        for (i in 1 until numPoints) {
            x_[i] = xs!![i]
            y_[i] = ys!![i]
            t_[i] = distance + MathUtil.distance(x_[i - 1], y_[i - 1], x_[i], y_[i])
            distance = t_[i]
        }

        this.fast = fast

        //            if( !fast)
        //                MDebug.handleWarning(MDebug.WarningType.UNSUPPORTED, null, "Precise smoothing not implemented.");

        fastCalculateSlopes()
    }

    override val curveLength: Float get() = this.distance

    fun getX(n: Int): Float {
        return x_[n]
    }

    fun getY(n: Int): Float {
        return y_[n]
    }

    override fun addPoint(x: Float, y: Float) {
        if (kx.size <= numPoints) expand(numPoints + 1)

        // Code could be made less verbose in by combining parts of the
        //	different cases, but would be less readable
        x_[numPoints] = x
        y_[numPoints] = y
        when (numPoints) {
            0 -> t_[0] = 0f
            1 -> {
                t_[1] = MathUtil.distance(x_[0], y_[0], x_[1], y_[1])
                val dt = t_[1] - t_[0]
                kx[1] = 0.25f * (x_[1] - x_[0]) / dt
                ky[1] = 0.25f * (y_[1] - y_[0]) / dt
                kx[0] = kx[1]
                ky[0] = ky[1]
            }
            else -> {
                x_[numPoints] = x
                y_[numPoints] = y
                t_[numPoints] = t_[numPoints - 1] + MathUtil.distance(x_[numPoints - 1], y_[numPoints - 1], x_[numPoints], y_[numPoints])

                val dt1 = t_[numPoints] - t_[numPoints - 1]
                val dt2 = t_[numPoints - 1] - t_[numPoints - 2]
                kx[numPoints - 1] = 0.5f * ((x_[numPoints] - x_[numPoints - 1]) / dt1 + (x_[numPoints - 1] - x_[numPoints - 2]) / dt2)
                ky[numPoints - 1] = 0.5f * ((y_[numPoints] - y_[numPoints - 1]) / dt1 + (y_[numPoints - 1] - y_[numPoints - 2]) / dt2)

                kx[numPoints] = 0.25f * (x_[numPoints] - x_[numPoints - 1]) / dt1
                ky[numPoints] = 0.25f * (y_[numPoints] - y_[numPoints - 1]) / dt1
            }
        }

        distance = t_[numPoints]
        numPoints++
    }

    /** Expands the internal arrays in order to accommodate the new length.  */
    private fun expand(new_length: Int) {
        if (kx.size >= new_length) return

        var l = if (numPoints == 0) new_length else kx.size

        // Expand by 50% at a time (similar to ArrayList)
        // TODO: Not using ArrayList to avoid boxing/unboxing bloar, but
        //	should still encapsulate this stuff in a custom primitive list
        //	object
        while (l < new_length)
            l = (l * 3 + 1) / 2

        var buff = FloatArray(l)
        VectrixMathLayer.arraycopy(kx, 0, buff, 0, numPoints)
        kx = buff
        buff = FloatArray(l)
        VectrixMathLayer.arraycopy(ky, 0, buff, 0, numPoints)
        ky = buff
        buff = FloatArray(l)
        VectrixMathLayer.arraycopy(x_, 0, buff, 0, numPoints)
        x_ = buff
        buff = FloatArray(l)
        VectrixMathLayer.arraycopy(y_, 0, buff, 0, numPoints)
        y_ = buff
        buff = FloatArray(l)
        VectrixMathLayer.arraycopy(t_, 0, buff, 0, numPoints)
        t_ = buff
    }

    /** Calculates the slopes using the simple Finite-Distance method which
     * just takes the average of the vector to and away from a middle point
     */
    private fun fastCalculateSlopes() {
        if (numPoints <= 1) return

        // Note: Enpoint weighting is suppressed a little to avoid wonky
        //	start/end curves
        var dt = t_[1] - t_[0]
        kx[0] = 0.25f * (x_[1] - x_[0]) / dt
        ky[0] = 0.25f * (y_[1] - y_[0]) / dt

        var i = 1
        while (i < numPoints - 1) {
            val dt1 = t_[i + 1] - t_[i]
            val dt2 = t_[i] - t_[i - 1]
            kx[i] = 0.5f * ((x_[i + 1] - x_[i]) / dt1 + (x_[i] - x_[i - 1]) / dt2)
            ky[i] = 0.5f * ((y_[i + 1] - y_[i]) / dt1 + (y_[i] - y_[i - 1]) / dt2)
            ++i

        }
        dt = t_[i] - t_[i - 1]
        kx[i] = 0.25f * (x_[i] - x_[i - 1]) / dt
        ky[i] = 0.25f * (y_[i] - y_[i - 1]) / dt
    }


    override fun eval(t: Float): Vec2f {
        if (numPoints == 0) return Vec2f(0f, 0f)

        if (t <= 0 && !isExtrapolating) return Vec2f(x_[0], y_[0])
        if (t >= distance && !isExtrapolating) return Vec2f(x_[numPoints - 1], y_[numPoints - 1])

        var i = 0
        while (t > t_[i] && ++i < numPoints);
        if (i == numPoints && !isExtrapolating) return Vec2f(x_[numPoints - 1], y_[numPoints - 1])

        if (i == 0) {
            val p1 = _eval(0.075f, 0, 1)

            val dt = MathUtil.distance(p1.xf, p1.yf, x_[0], y_[0])
            val d = t / dt

            return Vec2f(x_[0] + d * (p1.xf - x_[0]), y_[0] + d * (p1.yf - y_[0]))
        }
        if (i == numPoints) {

            val p1 = _eval(0.925f, numPoints - 2, numPoints - 1)

            val dt = MathUtil.distance(p1.xf, p1.yf, x_[numPoints - 1], y_[numPoints - 1])
            val d = (t - distance) / dt + 1

            return Vec2f(x_[numPoints - 1] + d * (x_[numPoints - 1] - p1.xf), y_[numPoints - 1] + d * (y_[numPoints - 1] - p1.yf))
        }

        return _eval((t - t_[i - 1]) / (t_[i] - t_[i - 1]), i - 1, i)
    }

    private fun _eval(n: Float, i1: Int, i2: Int): Vec2f {
        val dt = t_[i2] - t_[i1]
        val a_x = kx[i1] * dt - (x_[i2] - x_[i1])
        val b_x = -kx[i2] * dt + x_[i2] - x_[i1]
        val a_y = ky[i1] * dt - (y_[i2] - y_[i1])
        val b_y = -ky[i2] * dt + y_[i2] - y_[i1]

        val qx = (1 - n) * x_[i1] + n * x_[i2] + n * (1 - n) * (a_x * (1 - n) + b_x * n)
        val qy = (1 - n) * y_[i1] + n * y_[i2] + n * (1 - n) * (a_y * (1 - n) + b_y * n)

        return Vec2f(qx, qy)
    }

    override fun evalExt(t: Float): Interpolator2D.InterpolatedPoint {
        if (kx.isEmpty()) return Interpolator2D.InterpolatedPoint(0f, 0f, 0f, 0, 0)

        if (t <= 0) return Interpolator2D.InterpolatedPoint(x_[0], y_[0], 0f, 0, 1)
        if (t >= distance) return Interpolator2D.InterpolatedPoint(x_[kx.size - 1], y_[kx.size - 1], 1f, kx.size - 2, kx.size - 1)

        var i = 0
        while (t > t_[i] && ++i < kx.size);
        if (i == kx.size) return Interpolator2D.InterpolatedPoint(x_[kx.size - 1], y_[kx.size - 1], 1f, kx.size - 2, kx.size - 1)


        val dt = t_[i] - t_[i - 1]
        val n = (t - t_[i - 1]) / dt

        val a_x = kx[i - 1] * dt - (x_[i] - x_[i - 1])
        val b_x = -kx[i] * dt + x_[i] - x_[i - 1]
        val a_y = ky[i - 1] * dt - (y_[i] - y_[i - 1])
        val b_y = -ky[i] * dt + y_[i] - y_[i - 1]

        val qx = (1 - n) * x_[i - 1] + n * x_[i] + n * (1 - n) * (a_x * (1 - n) + b_x * n)
        val qy = (1 - n) * y_[i - 1] + n * y_[i] + n * (1 - n) * (a_y * (1 - n) + b_y * n)

        return Interpolator2D.InterpolatedPoint(qx, qy, n, i - 1, i)
    }
}