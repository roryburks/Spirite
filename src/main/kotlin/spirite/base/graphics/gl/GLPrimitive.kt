package spirite.base.graphics.gl

import spirite.base.util.glu.GLC

enum class PolyType( val glConst: Int) {
    STRIP( GLC.TRIANGLE_STRIP),
    FAN(GLC.TRIANGLE_FAN),
    LIST(GLC.TRIANGLE_STRIP)
}

data class GLPrimitive(
        val raw: FloatArray,
        val attrLengths: IntArray,
        val primitiveTypes: IntArray,
        val primitiveLengths: IntArray
) {
    constructor( raw: FloatArray, attrLengths: IntArray, primitiveType: Int, primitiveLengths: IntArray) :
            this(raw, attrLengths, IntArray(primitiveLengths.size, {primitiveType}), primitiveLengths)
}

internal class PreparedPrimitive(
        val primative: GLPrimitive,
        val gle: GLEngine
) {
    val gl = gle.gl
    val buffer = gl.createBuffer() ?: throw GLEException("Failed to create Buffer")

    init {
        gl.bindBuffer(GLC.ARRAY_BUFFER, buffer)
        gl.bufferData(GLC.ARRAY_BUFFER, gle.gl.makeFloat32Source(primative.raw), GLC.STREAM_DRAW)
        gl.bindBuffer(GLC.ARRAY_BUFFER, null)
    }

    fun use() {
        gl.bindBuffer(GLC.ARRAY_BUFFER, buffer)

        val lengths = primative.attrLengths
        val totalLength = lengths.sum()
        for( i in 0 until primative.attrLengths.size)
            gl.enableVertexAttribArray(i)
        var offset = 0
        for( i in 0 until lengths.size) {
            gl.vertexAttribPointer(i, lengths[i], GLC.FLOAT, false, 4*totalLength, 4*offset)
            offset += lengths[i]
        }
    }

    fun unuse() {
        for( i in 0 until primative.attrLengths.size)
            gl.disableVertexAttribArray(i)
    }

    fun flush() {
        buffer.delete()
    }
}