package rb.vectrix.interpolation

import rb.vectrix.VectrixMathLayer
import kotlin.math.max

private const val SIZE_PER = 10

class CubicSplineInterpolatorND(
        points: Array<FloatArray>,
        val N: Int,
        length: Int,
        val fast: Boolean = true
) {
    var length = length ; private set
    private val dx : Array<FloatArray>
    private val x_ : Array<FloatArray>
    private var t_ : FloatArray

    init {
        val l = max(SIZE_PER, length)

        dx = Array(N) { FloatArray(l) }
        x_ = Array(N) { FloatArray(l) }
        t_ = FloatArray(l)

        (0 until length).forEach { w->
            (0 until N).forEach { i -> x_[i][w] = points[w][i]}
            t_[w] = points[w][N]
        }
    }

    init {
        // CalculateDifferentials
        if( length > 1) {
            val dt_0 = t_[1] - t_[0]
            (0 until N).forEach { dx[it][0] = 0.25f * (x_[it][1] - x_[it][0])/dt_0 }

            (0 until length-1).forEach { w->
                val dt_1 = t_[w+1] - t_[w]
                val dt_2 = t_[w] - t_[w-1]
                (0 until N).forEach { dx[it][w] = 0.5f * ((x_[it][w+1]-x_[it][w])/dt_1 + (x_[it][w]-x_[it][w-1])/dt_2) }
            }

            val dt_n = t_[length] - t_[length-1]
            (0 until N).forEach { dx[it][length] = 0.25f * (x_[it][length] - x_[it][length-1])/dt_n }
        }
    }

    fun addPoint(p: FloatArray) {
        if (x_.size <= length) expand(length + 1)

        // Code could be made less verbose in by combining parts of the
        //	different cases, but would be less readable
        (0 until N).forEach { x_[it][length] = p[it]}
        t_[length] = p[N]

        when( length) {
            0 -> t_[0] = 0f
            1 -> {
                val dt = t_[1] - t_[0]
                for (i in 0 until N) {
                    dx[i][1] = 0.25f * (x_[i][1] - x_[i][0]) / dt
                    dx[i][0] = dx[i][1]
                }
            }
            else -> {
                val dt1 = t_[length] - t_[length - 1]
                val dt2 = t_[length - 1] - t_[length - 2]

                for (i in 0 until N) {
                    dx[i][length - 1] = 0.5f * ((x_[i][length] - x_[i][length - 1]) / dt1 + (x_[i][length - 1] - x_[i][length - 2]) / dt2)
                    dx[i][length] = 0.25f * (x_[i][length] - x_[i][length - 1]) / dt1
                }
            }
        }

        length++
    }

    /** Expands the internal arrays in order to accommodate the new length.  */
    private fun expand(new_length: Int) {
        if (x_.size >= new_length) return

        var l = if (length == 0) new_length else x_.size

        // Expand by 50% at a time (similar to ArrayList)
        // TODO: Not using ArrayList to avoid boxing/unboxing bloat, but
        //	should still encapsulate this stuff in a custom primitive list
        //	object
        while (l < new_length)
            l = (l * 3 + 1) / 2

        var buff: FloatArray
        for (i in 0 until N) {
            buff = FloatArray(l)
            VectrixMathLayer.arraycopy(x_[i], 0, buff, 0, length)
            x_[i] = buff
            buff = FloatArray(l)
            VectrixMathLayer.arraycopy(dx[i], 0, buff, 0, length)
            dx[i] = buff
        }
        buff = FloatArray(l)
        VectrixMathLayer.arraycopy(t_, 0, buff, 0, length)
        t_ = buff
    }


    fun eval(t: Float): FloatArray {
        val ret = FloatArray(N)
        if (length == 0) return ret

        if (t <= 0) {
            (0 until N).forEach { ret[it] = x_[it][0]}
            return ret
        }

        var w = 1
        while (t > t_[w] && ++w < length);
        if (w == length) {
            (0 until N).forEach {ret[it] = x_[it][length - 1]}
            return ret
        }

        val dt = t_[w] - t_[w - 1]
        val n = (t - t_[w - 1]) / dt

        for (i in 0 until N) {
            val a = dx[i][w - 1] * dt - (x_[i][w] - x_[i][w - 1])
            val b = -dx[i][w] * dt + x_[i][w] - x_[i][w - 1]
            ret[i] = (1 - n) * x_[i][w - 1] + n * x_[i][w] + n * (n - 1) * (a * (1 - n) + b * n)
        }

        return ret
    }
}