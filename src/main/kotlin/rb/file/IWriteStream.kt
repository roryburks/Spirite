package rb.file

import rb.vectrix.mathUtil.b
import java.nio.charset.Charset

interface IWriteStream {
    val pointer: Long
    fun goto(pointer: Long)

    fun write(byteArray: ByteArray)
    fun writeInt(i: Int)
    fun writeByte(b: Int)
    fun writeFloat(f: Float)
    fun writeShort(s: Int)
    fun writeFloatArray(fa: FloatArray)
    fun writeStringUft8Nt(str: String)
}

class BigEndianWriteStream(val underlying: IRawWriteStream) :IWriteStream {

    // Delegated
    override val pointer get() = underlying.pointer
    override fun goto(pointer: Long) = underlying.goto(pointer)
    override fun write(byteArray: ByteArray) = underlying.write(byteArray)

    // Int
    fun <T> write(t: T, inter: IBinaryInterpreter<T>) {
        val ba = inter.convert(t)
        write(ba)
    }

    override fun writeInt(i: Int) = write(i, LittleEndian.IntInter)
    override fun writeByte(b: Int) = write(b.b, ByteInter)
    override fun writeFloat(f: Float) = write(f, LittleEndian.FloatInter)
    override fun writeShort(s: Int) = write(s.toShort(), LittleEndian.ShortInter)
    override fun writeFloatArray(fa: FloatArray) = write(fa, LittleEndian.FloatArrayInter(fa.size))
    override fun writeStringUft8Nt(str: String) {
        val b = (str + 0.toChar()).toByteArray(Charset.forName("UTF-8"))

        // Convert non-terminating null characters to whitespace
        val nil : Byte = 0
        for( i in 0 until b.size-1) {
            if( b[i] == nil)
                b[i] = 0x20
        }
        write(b)
    }


}