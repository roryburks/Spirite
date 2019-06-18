package rb.vectrix.shapes

import rb.glow.gl.GLC
import rb.glow.gle.GLPrimitive
import rb.vectrix.VectrixMathLayer
import rb.vectrix.compaction.FloatCompactor
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max


// TODO: Make not-dependent on JVM
data class OvalShape(
        val x : Float,
        val y: Float,
        val r_h: Float,
        val r_v: Float
) : IShape {
    fun buildPrimitive(maxError: Float, attrLengths: IntArray, packer: (x: Float, y: Float, writer: FloatCompactor) -> Unit): GLPrimitive {
        val compactor = FloatCompactor()

        packer(x,y,compactor)
        doAlongPath(maxError) { x, y ->
            packer(x.f,y.f,compactor)
        }

        return GLPrimitive(compactor.toArray(), attrLengths, GLC.TRIANGLE_FAN, intArrayOf(compactor.size))
    }
    override fun buildPath(maxError: Float): Pair<FloatArray, FloatArray> {
        val xComp = FloatCompactor()
        val yComp = FloatCompactor()
        doAlongPath(maxError) { x, y ->
            xComp.add(x.f)
            yComp.add(y.f)
        }
        return Pair(xComp.toArray(), yComp.toArray())
    }

    override fun doAlongPath(maxError: Float, lambda: (x: Double, y: Double) -> Unit) {
        val c = 1 - abs(maxError) / max(r_h, r_v)
        val theta_d = when {
            c < 0 -> PI/2.0
            else -> acos(c.toDouble())
        }

        var theta = 0.0
        while( theta < 2*PI) {
            val x = (x + r_h * VectrixMathLayer.fastCos(theta))
            val y = (y + r_v * VectrixMathLayer.fastSin(theta))

            lambda(x,y)
            theta += theta_d
        }
        lambda(x + r_h + 0.0 , y + 0.0)
    }

    override fun contains(x: Float, y: Float): Boolean {
        return MathUtil.distance(0.0, 0.0, (x - this.x) / r_h + 0.0, (y - this.y) / r_v + 0.0) <= 1
    }

}