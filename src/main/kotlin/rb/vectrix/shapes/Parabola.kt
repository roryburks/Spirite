package rb.vectrix.shapes

import kotlin.math.max
import kotlin.math.min


// y = Ax^2 + Bx + C
abstract class Parabola
{
    abstract val A: Double
    abstract val B: Double
    abstract val C: Double
    abstract val x1: Double
    abstract val x2: Double

    abstract val yB: Double
    abstract val yT: Double

    fun apply(x: Double) = A*x*x + B*x + C
}

data class ParabolaD
private constructor(
    override val A: Double,
    override val B: Double,
    override val C: Double,
    override val x1: Double,
    override val x2: Double

):Parabola()
{
    override val yB by lazy { listOf(apply(x1), apply(x2), apply(-B/(2*A))).min()!! }
    override val yT by lazy { listOf(apply(x1), apply(x2), apply(-B/(2*A))).max()!! }

    companion object {
        fun Make( A: Double, B: Double, C: Double, x1: Double, x2: Double) =
                ParabolaD(A,B,C, min(x1, x2), max(x1,x2))
    }

}