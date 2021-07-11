package rbJvm.file

import rb.file.IBinaryReadStream
import rb.vectrix.mathUtil.i
import java.io.RandomAccessFile

class JvmRandomAccessFileBinaryReadStream (val ba: RandomAccessFile) : IBinaryReadStream {
    override fun readInto(byteArray: ByteArray, offset: Int, length: Int) : Int {
        return ba.read(byteArray, offset, length)
    }

    override var filePointer: Long
        get() = ba.filePointer
        set(value) {ba.seek(value)}
    val len: Long get() = ba.length()

    override val eof: Boolean get() = len == ba.filePointer

    override fun close() { ba.close() }
}