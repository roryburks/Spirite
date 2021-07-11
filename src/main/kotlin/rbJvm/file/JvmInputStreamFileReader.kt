package rbJvm.file

import rb.file.IBinaryReadStream
import java.io.InputStream

class JvmInputStreamFileReader(val i: InputStream) : IBinaryReadStream {
    private var caret = 0L

    override fun readInto(byteArray: ByteArray, offset: Int, length: Int): Int {
        val readLen = i.read(byteArray, offset, length)
        caret += readLen
        return readLen
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

    // from Java Docs, InputStream available() should return "0 when it reaches the end of the input stream"
    override val eof: Boolean get() = (i.available() == 0)

    override fun close() { i.close() }
}