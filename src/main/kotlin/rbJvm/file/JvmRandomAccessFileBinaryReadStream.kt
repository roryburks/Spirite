package rbJvm.file

import rb.file.IBinaryReadStream
import rb.vectrix.mathUtil.i
import java.io.RandomAccessFile

class JvmRandomAccessFileBinaryReadStream (val ba: RandomAccessFile) : IBinaryReadStream {
    override fun readBytes(size: Int): ByteArray {
        val data = ByteArray(size)
        ba.read(data)
        return data
    }

    override fun readInto(byteArray: ByteArray, offset: Int, length: Int) {
        ba.read(byteArray, offset, length)
    }

    override var filePointer: Long
        get() = ba.filePointer
        set(value) {ba.seek(value)}
    val len: Int get() = ba.length().i

}