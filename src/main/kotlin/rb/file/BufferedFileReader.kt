package rb.file

import rb.vectrix.IMathLayer
import rb.vectrix.VectrixMathLayer

class BufferedFileReader(
        val stream: IBinaryReadStream,
        val bufferSize: Int = 1024,
        val mathLayer: IMathLayer = VectrixMathLayer.mathLayer)
    : IFileReader
{
    var _buffer : ByteArray? = null
    var _bCarat : Int = 0
    private var _pointer: Long = 0


    fun <T> read(inter : IBinaryInterpreter<T>) : T{
        val len = inter.len
        val data = ByteArray(len)
        readInto(data, 0)
        _pointer += len
        return inter.interpret(data)
    }

    private fun readInto(data: ByteArray, offset: Int) {
        val len = data.size - offset
        val buffer = _buffer

        if( buffer == null) {
            if( len >= bufferSize) {
                stream.readInto(data, offset, len)
            }
            else {
                val newBuff = ByteArray(bufferSize)
                stream.readInto(newBuff, 0, bufferSize)
                mathLayer.arraycopy(newBuff, 0, data, offset, len)
                _buffer = newBuff
                _bCarat = len
            }
        }
        else {
            val sizeLeft = bufferSize - _bCarat
            if( sizeLeft  >= len){
                mathLayer.arraycopy(buffer, _bCarat, data, offset, len)
                _bCarat += len
                if( _bCarat == bufferSize) {
                    _bCarat = 0
                    _buffer = null
                }
            }
            else {
                mathLayer.arraycopy(buffer, _bCarat, data, offset, sizeLeft)
                _bCarat = 0
                _buffer = null
                readInto(data, offset + (sizeLeft))
            }
        }
    }

    override fun readShort() = read(LittleEndian.ShortInter)
    override fun readUnsignedShort() = read(LittleEndian.UShortInter)
    override fun readByte() = read(ByteInter)
    override fun readInt() = read(LittleEndian.IntInter)
    override fun readFloat() = read(LittleEndian.FloatInter)
    override fun readUnsignedByte() = read(LittleEndian.UByteInter)
    override fun readFloatArray(size: Int) = read(LittleEndian.FloatArrayInter(size))
    override fun readByteArray(size: Int) = read(ByteArrayInter(size))

    override var filePointer: Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

}