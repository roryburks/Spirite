package rb.file

import rb.vectrix.mathUtil.i
import java.io.BufferedReader
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
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override val len: Int get() = ba.length().i

}