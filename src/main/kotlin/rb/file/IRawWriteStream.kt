package rb.file

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
        byteArray.forEach { byte ->
            if( _pointer > list.size || _pointer < 0)
                throw IndexOutOfBoundsException("Pointer out of bounts.  Pointer: $_pointer List Size: ${list.size}")
            if( _pointer == list.size)
                list.add(byte)
            else
                list[_pointer] = byte

           ++_pointer
        }
//        (start until start + len).forEach {
//            val v = list[it]
//            when (val p = _pointer++) {
//                in (0 until list.size) -> list[p] = v
//                else -> list.add(v)
//            }
//        }
    }
    override fun finish() { }
    override fun close() { }
}