package rbJvm.file

import rb.file.IBinaryReadStream
import java.io.InputStream

class JvmInputStreamFileReader(val i: InputStream) : IBinaryReadStream {
    override fun readBytes(size: Int): ByteArray {
        return i.readNBytes(size)
    }

    override fun readInto(byteArray: ByteArray, offset: Int, length: Int) {
        i.readNBytes(byteArray, offset, length)
    }

    override var filePointer: Long
        get() = TODO("Not yet implemented")
        set(value) {}
    override val len: Int
        get() = TODO("Not yet implemented")
}