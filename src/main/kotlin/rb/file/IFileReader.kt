package rb.file

import rb.vectrix.IMathLayer
import rb.vectrix.VectrixMathLayer
import rb.vectrix.mathUtil.i
import java.io.ByteArrayOutputStream
import kotlin.math.min

interface IFileReader {
    fun readShort() : Short
    fun readUnsignedShort() : Int
    fun readByte() : Byte
    fun readInt() : Int
    fun readFloat() : Float
    fun readUnsignedByte() : Int

    fun readFloatArray( size: Int) : FloatArray
    fun readByteArray( size: Int) : ByteArray

    var filePointer: Long
}

fun IFileReader.readUtf8(): String {
    val bos = ByteArrayOutputStream()
    var b = this.readByte()
    while( b != 0x00.toByte()) {
        bos.write(b.i)
        b = this.readByte()
    }

    return bos.toString("UTF-8")
}

interface IBinaryReadStream {
    fun readBytes( size: Int) : ByteArray
    fun readInto( byteArray: ByteArray, offset: Int, length: Int)
    var filePointer : Long
}

class ByteArrayReadStream(
        val data: ByteArray,
        val mathLayer: IMathLayer = VectrixMathLayer.mathLayer
) : IBinaryReadStream
{
    override fun readBytes(size: Int): ByteArray {
        val array = ByteArray(size)
        readInto(array, 0, array.size)
        return array
    }

    override fun readInto(byteArray: ByteArray, offset: Int, length: Int) {
        val aLen = min(data.size - filePointer.i, length)
        mathLayer.arraycopy(data, filePointer.i, byteArray, offset, aLen)
        filePointer += aLen
    }

    override var filePointer: Long = 0
    val len: Int get() = data.size
}
