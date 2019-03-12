package spirite.base.util.shapes

import com.hackoeur.jglm.support.FastMath
import rb.vectrix.compaction.FloatCompactor
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import spirite.base.graphics.gl.GLPrimitive
import spirite.base.util.glu.GLC
import kotlin.math.PI


// TODO: Merge with Vectrix Shapes
data class Oval(
        val x : Float,
        val y: Float,
        val r_h: Float,
        val r_v: Float
) : IShape {
    override fun buildPrimitive(maxError: Float, attrLengths: IntArray, packer: (x: Float, y: Float, writer: FloatCompactor) -> Unit): GLPrimitive {
        val compactor = FloatCompactor()

        packer(x,y,compactor)
        doAlongPath(maxError) { x, y ->
            packer(x.f,y.f,compactor)
        }

        return GLPrimitive(compactor.toArray(), attrLengths, GLC.TRIANGLE_FAN, intArrayOf(compactor.size))
    }

    override fun doAlongPath(maxError: Float, lambda: (x: Double, y: Double) -> Unit) {
        val c = 1 - Math.abs(maxError) / Math.max(r_h, r_v)
        val theta_d = when {
            c < 0 -> PI/2.0
            else -> Math.acos(c.toDouble())
        }

        var theta = 0.0
        while( theta < 2*PI) {
            val x = (x + r_h * FastMath.cos(theta))
            val y = (y + r_v * FastMath.sin(theta))

            lambda(x,y)
            theta += theta_d
        }
        lambda(x + r_h + 0.0 , y + 0.0)
    }

    override fun contains(x: Float, y: Float): Boolean {
        return MathUtil.distance(0.0, 0.0, (x - this.x) / r_h + 0.0, (y - this.y) / r_v + 0.0) <= 1
    }

}