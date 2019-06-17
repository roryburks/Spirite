package rb.vectrix.shapes

interface IShape {
//    fun buildPrimitive(
//            maxError: Float = 0.2f,
//            attrLengths: IntArray = intArrayOf(2),
//            packer : (x: Float, y: Float, writer: FloatCompactor) -> Unit = { x, y, writer -> writer.add(x) ; writer.add(y)})
//            : GLPrimitive

    fun buildPath( maxError: Float) : Pair<FloatArray,FloatArray>
    fun doAlongPath( maxError: Float, lambda : (x: Double, y: Double) -> Unit )

    fun contains( x: Float, y: Float) : Boolean
}