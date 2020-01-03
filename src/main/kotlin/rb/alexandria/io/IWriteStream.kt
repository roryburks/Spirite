package rb.alexandria.io

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