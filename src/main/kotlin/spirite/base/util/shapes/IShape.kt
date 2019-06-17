package spirite.base.util.shapes

import rb.glow.gle.GLPrimitive
import rb.vectrix.mathUtil.f
import rbJvm.vectrix.compaction.FloatCompactor

// TODO: Merge with Vectrix Shapes
interface IShape {
    fun buildPrimitive(
            maxError: Float = 0.2f,
            attrLengths: IntArray = intArrayOf(2),
            packer : (x: Float, y: Float, writer: FloatCompactor) -> Unit = { x, y, writer -> writer.add(x) ; writer.add(y)})
            : GLPrimitive

    fun buildPath( maxError: Float) : Pair<FloatArray,FloatArray> {
        val xComp = FloatCompactor()
        val yComp = FloatCompactor()
        doAlongPath(maxError) { x, y ->
            xComp.add(x.f)
            yComp.add(y.f)
        }
        return Pair(xComp.toArray(), yComp.toArray())
    }
    fun doAlongPath( maxError: Float, lambda : (x: Double, y: Double) -> Unit )

    fun contains( x: Float, y: Float) : Boolean
}