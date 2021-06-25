package rb.file

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

    //
    override fun writeInt(i: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeByte(b: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeFloat(f: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeShort(s: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeFloatArray(fa: FloatArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writeStringUft8Nt(str: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // Explicit



}