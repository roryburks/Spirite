package rbJvm.vectrix

import rb.vectrix.IMathLayer

object JvmMathLayer : IMathLayer {
    override fun arraycopy(src: IntArray, srcPos: Int, dest: IntArray, destPos: Int, len: Int) =
            System.arraycopy(src, srcPos, dest, destPos, len)

    override fun arraycopy(src: FloatArray, srcPos: Int, dest: FloatArray, destPos: Int, len: Int) =
            System.arraycopy(src, srcPos, dest, destPos, len)

}