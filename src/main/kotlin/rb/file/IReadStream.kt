package rb.file

import rb.vectrix.IMathLayer
import rb.vectrix.VectrixMathLayer
import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.l
import java.io.ByteArrayOutputStream
import kotlin.math.min

interface IReadStream {
    fun readShort() : Short
    fun readUnsignedShort() : Int
    fun readByte() : Byte
    fun readInt() : Int
    fun readFloat() : Float
    fun readUnsignedByte() : Int

    fun readFloatArray( size: Int) : FloatArray
    fun readByteArray( size: Int) : ByteArray

    var filePointer: Long
    val eof: Boolean
    fun close()
}

fun IReadStream.readStringUtf8(): String {
    val bos = ByteArrayOutputStream()
    var b = this.readByte()
    while( b != 0x00.toByte()) {
        bos.write(b.i)
        b = this.readByte()
    }

    return bos.toString("UTF-8")
}

interface IBinaryReadStream {
    fun readInto( byteArray: ByteArray, offset: Int = 0, length: Int = byteArray.size) : Int
    var filePointer : Long
    val eof: Boolean

    fun close()
}

class ByteArrayReadStream(
        val data: ByteArray,
        private val _mathLayer: IMathLayer = VectrixMathLayer.mathLayer
) : IBinaryReadStream
{
    override fun readInto(byteArray: ByteArray, offset: Int, length: Int) : Int {
        val aLen = min(data.size - filePointer.i, length)
        _mathLayer.arraycopy(data, filePointer.i, byteArray, offset, aLen)
        filePointer += aLen
        return aLen
    }

    override var filePointer: Long = 0
    val len: Int get() = data.size
    override val eof: Boolean get() = (filePointer == len.l)

    override fun close() {}
}
