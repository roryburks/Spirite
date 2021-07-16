package rb.file

import rb.vectrix.IMathLayer
import rb.vectrix.VectrixMathLayer

class BufferedReadStream(
    val underlying: IBinaryReadStream,
    val bufferSize: Int = 1024,
    val mathLayer: IMathLayer = VectrixMathLayer.mathLayer)
    : IReadStream
{
    // If _buffer is null, pointer = underlying.pointer, next reads come direct from underlying
    // If _buffer is not null, it means the buffer has been read from, so pointer = underlying.pointer - _buffer.size + _bCaret
    var _buffer : ByteArray? = null // non-private for hacky reasons that should be resolved with IO refactor.  7-1-2021
    var _bCarat : Int = 0 // ditto
    var _bufferValidSize = 0 // If _buffer is non-null this should be bufferSize or we're reaching eof

    override val eof: Boolean get() = _buffer == null && underlying.eof

    override var filePointer: Long
        get() = when(val buffer = _buffer ){
            null -> underlying.filePointer
            else -> underlying.filePointer - _bufferValidSize + _bCarat
        }
        set(value) {
            // For simplicity sake, don't do inter-buffer navigation
            _buffer = null
            _bCarat = 0
            underlying.filePointer = value
        }

    fun <T> read(inter : IBinaryInterpreter<T>) : T{
        val len = inter.len
        val data = ByteArray(len)
        readInto(data, 0)
        return inter.interpret(data)
    }

    private fun readInto(data: ByteArray, offset: Int) {
        val len = data.size - offset
        val buffer = _buffer

        if( buffer == null) {
            if( len >= bufferSize) {
                underlying.readInto(data, offset, len)
                // buffer remains null
                // TODO: Stuff if we start returning nulls/count when EoF
            }
            else {
                // NOTE: This is the only place in code _buffer gets set to non-null
                val newBuff = ByteArray(bufferSize)
                _bufferValidSize = underlying.readInto(newBuff, 0, bufferSize)
                if( _bufferValidSize == 0) {
                    // TODO: Stuff if we start returning nulls/count when EoF
                    return
                }
                mathLayer.arraycopy(newBuff, 0, data, offset, len)
                _buffer = newBuff
                _bCarat = len
                // TODO: Stuff if we start returning nulls/count when EoF
            }
        }
        else {
            val sizeLeft = _bufferValidSize - _bCarat
            if( sizeLeft  >= len){
                mathLayer.arraycopy(buffer, _bCarat, data, offset, len)
                _bCarat += len
                if( _bCarat == _bufferValidSize) {
                    _bCarat = 0
                    _buffer = null
                }
            }
            else {
                mathLayer.arraycopy(buffer, _bCarat, data, offset, sizeLeft)
                _bCarat = 0
                _buffer = null
                if( _bufferValidSize == bufferSize )
                    readInto(data, offset + (sizeLeft))
                // TODO: else for if we start returning nulls/count when EoF
            }
        }
    }

    override fun readShort() = read(BigEndian.ShortInter)
    override fun readUnsignedShort() = read(BigEndian.UShortInter)
    override fun readByte() = read(ByteInter)
    override fun readInt() = read(BigEndian.IntInter)
    override fun readFloat() = read(BigEndian.FloatInter)
    override fun readUnsignedByte() = read(BigEndian.UByteInter)
    override fun readFloatArray(size: Int) = read(BigEndian.FloatArrayInter(size))
    override fun readByteArray(size: Int) = read(ByteArrayInter(size))


    override fun close() { underlying.close() }
}