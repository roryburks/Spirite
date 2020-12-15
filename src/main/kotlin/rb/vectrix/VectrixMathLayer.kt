package rb.vectrix

import kotlin.math.cos
import kotlin.math.sin

object VectrixMathLayer {
    var mathLayer : IMathLayer = BaseMathLayer

    fun arraycopy(src: IntArray, srcPos: Int, dest:IntArray, destPos: Int, len: Int) = mathLayer.arraycopy(src, srcPos, dest, destPos, len)
    fun arraycopy(src: FloatArray, srcPos: Int, dest:FloatArray, destPos: Int, len: Int) = mathLayer.arraycopy(src, srcPos, dest, destPos, len)
    fun arraycopy(src: ByteArray, srcPos: Int, dest:ByteArray, destPos: Int, len: Int) = mathLayer.arraycopy(src, srcPos, dest, destPos, len)
    fun fastSin(theta: Double) = mathLayer.fastSin(theta)
    fun fastCos(theta: Double) = mathLayer.fastCos(theta)

}

interface IMathLayer {
    fun arraycopy(src: FloatArray, srcPos: Int, dest:FloatArray, destPos: Int, len: Int)
    fun arraycopy(src: IntArray, srcPos: Int, dest:IntArray, destPos: Int, len: Int)
    fun arraycopy(src: ByteArray, srcPos: Int, dest:ByteArray, destPos: Int, len: Int)

    fun fastSin(theta: Double) : Double
    fun fastCos(theta: Double) : Double
}

object BaseMathLayer : IMathLayer {

    override fun arraycopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, len: Int) {
        (0 until len).forEach { dest[destPos + it] = src[srcPos + len] }
    }

    override fun arraycopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, len: Int) {
        (0 until len).forEach { dest[destPos + it] = src[srcPos + len] }
    }

    override fun arraycopy(src: ByteArray, srcPos: Int, dest: ByteArray, destPos: Int, len: Int) {
        (0 until len).forEach { dest[destPos + it] = src[srcPos + len] }
    }

    override fun fastSin(theta: Double) = sin(theta)
    override fun fastCos(theta: Double) = cos(theta)
}