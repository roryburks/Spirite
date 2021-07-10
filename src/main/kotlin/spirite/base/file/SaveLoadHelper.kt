package spirite.base.file

import spirite.base.file.sif.SaveLoadUtil
import java.io.RandomAccessFile
import java.nio.ByteBuffer

fun RandomAccessFile.writeUFT8NT(str: String) {
    val bytes = SaveLoadUtil.strToByteArrayUTF8(str)
    write(bytes)
}

fun RandomAccessFile.readUTF8NT() = SaveLoadUtil.readNullTerminatedStringUTF8(this)

fun RandomAccessFile.writeFloatArray(floatArray: FloatArray) {
    val buf = ByteBuffer.allocate(floatArray.size * 4)
    buf.clear()
    buf.asFloatBuffer().put(floatArray)
    this.channel.write(buf)
}

fun RandomAccessFile.readFloatArray(len: Int) : FloatArray {
    val buf = ByteBuffer.allocate(len*4)
    buf.clear()
    channel.read(buf)
    buf.rewind()
    return FloatArray(len).also { buf.asFloatBuffer().get(it) }
}