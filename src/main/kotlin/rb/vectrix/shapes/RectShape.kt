package rb.vectrix.shapes

import rb.glow.gl.GLC
import rb.glow.gle.GLPrimitive
import rb.vectrix.compaction.FloatCompactor
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f

// TODO: Make not-dependent on JVM
class RectShape(
        val x : Float,
        val y: Float,
        val w: Float,
        val h: Float) : IShape
{
    fun buildPrimitive(maxError: Float, attrLengths: IntArray, packer: (x: Float, y: Float, writer: FloatCompactor) -> Unit): GLPrimitive {
        val compactor = FloatCompactor()
        packer(x, y,compactor)
        packer(x+w, y,compactor)
        packer(x, y+h,compactor)
        packer(x+w, y+h,compactor)
        return GLPrimitive(compactor.toArray(), attrLengths, GLC.TRIANGLE_STRIP, intArrayOf(4))
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
        lambda(x.d, y.d)
        lambda(x+w+0.0, y.d)
        lambda(x+w+0.0, y+h+0.0)
        lambda(x.d, y+h+0.0)
        lambda(x.d, y.d)
    }

    override fun contains(x: Float, y: Float): Boolean {
        return x >= this.x && y >= this.y && x <= this.x+w && y <= this.y+h
    }

}