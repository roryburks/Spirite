package rb.vectrix.interpolation

import rb.vectrix.linear.Vec2f

/** Constructs a Polygon of degree N-1 which goes through the given N
 * drawPoints and uses that Polygon to interpolate the data.
 *
 * Lagrange Interpolations are a straightforward form of interpolation
 * that results in an infinitely differentiable curve (meaning it is
 * extremely smooth), but creates a curve which can have a lot of hills
 * and valleys, resulting in a very erratic-looking curve.  */
class LagrangeInterpolator(points: List<Vec2f>) : Interpolator {
    private var coef: FloatArray

    init {
        val N = points.size
        coef = FloatArray(N)
        val pi_coef = FloatArray(N)
        val pi_coef2 = FloatArray(N)

        // Calculate the coefficience for the Lagrange polynomial
        for (i in 0 until N) {
            var divisor = 1f

            // Calculate the divisor of the coefficient
            val p_i = points[i]
            var p_j: Vec2f

            for (j in 0 until N) {
                if (j == i) continue
                p_j = points[j]
                divisor *= p_i.xf - p_j.xf
            }

            // Calculate the denominator coefficients that p_i contribute to the
            //	polynomial
            for (j in 0 until N) {
                pi_coef[i] = 0f
                pi_coef2[i] = 0f
            }
            pi_coef[0] = 1f
            for (j in 0 until N) {
                if (j == i) continue
                p_j = points[j]

                // * (xi - x_j)
                //	- x_j)
                for (k in 0 until N) {
                    pi_coef2[k] = pi_coef[k] * -p_j.xf
                }
                // (xi
                for (k in N - 2 downTo 0) {
                    pi_coef[k + 1] = pi_coef[k]
                }
                pi_coef[0] = 0f

                // combine the two
                for (k in 0 until N) {
                    pi_coef[k] += pi_coef2[k]
                }
            }
            // Add the calculated coefficients to the final coefficients
            for (j in 0 until N) {
                coef[j] += pi_coef[j] * p_i.yf / divisor
            }
        }
    }

    override fun eval(t: Float): Float {
        if (coef.size == 0) return 0f
        var ret = coef[0]
        var x_to_n = 1f

        for (i in 1 until coef.size) {
            x_to_n *= t
            ret += x_to_n * coef[i]
        }
        return ret
    }
}