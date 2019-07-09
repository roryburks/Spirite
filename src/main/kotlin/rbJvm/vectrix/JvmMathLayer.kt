package rbJvm.vectrix

import rb.vectrix.IMathLayer
import kotlin.math.cos
import kotlin.math.sin

object JvmMathLayer : IMathLayer {
    override fun fastSin(theta: Double) = sin(theta)
    override fun fastCos(theta: Double) = cos(theta)

    override fun arraycopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, len: Int) =
            System.arraycopy(src, srcPos, dest, destPos, len)

    override fun arraycopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, len: Int) =
            System.arraycopy(src, srcPos, dest, destPos, len)

}