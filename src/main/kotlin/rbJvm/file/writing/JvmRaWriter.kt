package rbJvm.file.writing

import rb.file.IFileWriter
import spirite.base.file.writeFloatArray
import java.io.RandomAccessFile

class JvmRaWriter(val ra: RandomAccessFile): IFileWriter {
    override fun writeInt(i: Int) { ra.writeInt(i)}
    override fun writeShort(i: Int) { ra.writeShort(i) }
    override fun writeByte(byte: Int) { ra.writeByte(byte) }
    override fun writeFloat(float: Float) { ra.writeFloat(float) }
    override fun writeBytes(bytes: ByteArray) { ra.write(bytes) }
    override fun writeFloats(data: FloatArray) { ra.writeFloatArray(data) }
}