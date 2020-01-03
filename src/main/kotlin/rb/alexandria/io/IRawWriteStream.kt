package rb.alexandria.io

import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.l

interface IRawWriteStream {
    val pointer: Long
    fun goto(pointer: Long)

    fun write(byteArray: ByteArray)
    fun finish()
    fun close()
}

class ByteListWriteStream() : IRawWriteStream {
    val list = mutableListOf<Byte>()
    private  var _pointer: Int = 0

    override val pointer: Long get() = _pointer.l
    override fun goto(pointer: Long) { _pointer = pointer.i }
    override fun write(byteArray: ByteArray) {
        byteArray.forEach {
            when (val p = _pointer++) {
                in (0 until list.size) -> list[p] = it
                else -> list.add(it)
            }
        }
    }
    override fun finish() { }
    override fun close() { }
}