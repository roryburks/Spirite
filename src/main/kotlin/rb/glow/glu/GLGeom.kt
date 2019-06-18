package rb.glow.glu

import rb.glow.gle.GLPrimitive
import rb.vectrix.compaction.FloatCompactor
import rb.vectrix.compaction.ReverseFloatCompactor

class PrimitiveBuilder(
        val attrLengths: IntArray,
        val primitiveType: Int
) {
    val output = FloatCompactor()
    val totalAttrLength = attrLengths.sum()
    var primitiveLengths = mutableListOf<Int>()
    var plen = 0

    fun emitVertex( vertexData: FloatArray) {
        (0 until totalAttrLength)
                .forEach { output.add( if( it < vertexData.size) vertexData[it] else 0.0f ) }
        plen++
    }

    fun emitPrimitive() {
        if( plen > 0)
            primitiveLengths.add(plen)
        plen = 0
    }

    fun build() = GLPrimitive(output.toArray(), attrLengths, primitiveType, primitiveLengths.toIntArray())
}
class DoubleEndedSinglePrimitiveBuilder(
        val attrLengths: IntArray,
        val primitiveType: Int
) {

    val forward = FloatCompactor()
    val backward = ReverseFloatCompactor()
    val totalAttrLength = attrLengths.sum()

    fun emitVertexFront( vertexData: FloatArray) {
        (0 until totalAttrLength)
                .forEach { forward.add( if( it < vertexData.size) vertexData[it] else 0.0f ) }
    }

    fun emitVertexBack( vertexData: FloatArray) {
        (0 until totalAttrLength)
                .forEach { backward.add( if( it < vertexData.size) vertexData[totalAttrLength - it - 1] else 0.0f ) }
    }

    fun build() : GLPrimitive {
        val raw = FloatArray( forward.size + backward.size)
        backward.insertIntoArray(raw, 0)
        forward.insertIntoArray(raw, backward.size)

        val primitiveTypes = intArrayOf(primitiveType)
        val primitiveLengths = intArrayOf(raw.size / attrLengths.sum())

        return GLPrimitive(raw, attrLengths, primitiveTypes, primitiveLengths)
    }
}