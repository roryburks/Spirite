package rb.glow.gle

import rb.glow.exceptions.GLEException
import rb.glow.gl.GLC
import rb.glow.gl.IGL
import kotlin.math.min

enum class PolyType( val glConst: Int) {
    STRIP( GLC.TRIANGLE_STRIP),
    FAN(GLC.TRIANGLE_FAN),
    LIST(GLC.TRIANGLE_STRIP)
}

interface IGLPrimitive
{
    fun prepare( gl: IGL) : IPreparedPrimitive
}
interface IPreparedPrimitive
{
    fun use()
    fun draw()
    fun unuse()
    fun flush()
}

data class GLPrimitive(
        val raw: FloatArray,
        val attrLengths: IntArray,
        val primitiveTypes: IntArray,
        val primitiveLengths: IntArray)
    : IGLPrimitive
{
    constructor( raw: FloatArray, attrLengths: IntArray, primitiveType: Int, primitiveLengths: IntArray) :
            this(raw, attrLengths, IntArray(primitiveLengths.size) {primitiveType}, primitiveLengths)

    override fun prepare(gl: IGL) = PreparedPrimitive(this, gl)
}

class GlCreateBufferException(msg: String) : GLEException(msg)
class PreparedPrimitive(
        val primative: GLPrimitive,
        val gl: IGL)
    : IPreparedPrimitive
{
    val buffer = gl.createBuffer() ?: throw GlCreateBufferException("Failed to create Buffer")

    init {
        gl.bindBuffer(GLC.ARRAY_BUFFER, buffer)
        gl.bufferData(GLC.ARRAY_BUFFER, gl.makeFloat32Source(primative.raw), GLC.STREAM_DRAW)
        gl.bindBuffer(GLC.ARRAY_BUFFER, null)
    }

    override fun use() {
        gl.bindBuffer(GLC.ARRAY_BUFFER, buffer)

        val lengths = primative.attrLengths
        val totalLength = lengths.sum()
        for (i in 0 until primative.attrLengths.size)
            gl.enableVertexAttribArray(i)
        var offset = 0
        for (i in 0 until lengths.size) {
            gl.vertexAttribPointer(i, lengths[i], GLC.FLOAT, false, 4 * totalLength, 4 * offset)
            offset += lengths[i]
        }
    }

    override fun draw() {
        var start = 0
        for (i in 0 until min(primative.primitiveLengths.size, primative.primitiveTypes.size)) {
            gl.drawArrays(primative.primitiveTypes[i], start, primative.primitiveLengths[i])
            start += primative.primitiveLengths[i]
        }
    }

    override fun unuse() {
        for (i in 0 until primative.attrLengths.size)
            gl.disableVertexAttribArray(i)
    }

    override fun flush() {
        buffer.delete()
    }
}