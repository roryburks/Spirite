package rbJvm.file

import rb.file.IBinaryReadStream
import java.io.InputStream

class JvmInputStreamFileReader(val i: InputStream) : IBinaryReadStream {
    private var caret = 0L

    override fun readBytes(size: Int): ByteArray {
        caret += size
        val ba = ByteArray(size)
        i.read(ba)
        return ba
    }

    override fun readInto(byteArray: ByteArray, offset: Int, length: Int) {
        caret += length
        val ba = ByteArray(length)
        i.read(ba, offset, length)
    }

    override var filePointer: Long
        get() = caret
        set(value) {
            if( value < caret) {
                i.reset()
                i.skip(value)
                caret = value
            }
            else if( value > caret) {
                i.skip(value - caret)
                caret = value
            }
        }
}