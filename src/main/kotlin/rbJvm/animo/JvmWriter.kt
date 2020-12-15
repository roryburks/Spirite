package rbJvm.animo

import rb.animo.io.IWriter
import java.io.RandomAccessFile

class JvmWriter(val ra: RandomAccessFile) : IWriter {
    override fun writeInt(i: Int) { ra.writeInt(i) }

    override fun writeShort(i: Int) {ra.writeShort(i) }

    override fun writeByte(i: Int) {ra.writeByte(i) }

    override fun writeUtf8(s: String) {
        val ba = FileUtil.strToByteArrayUTF8(s)
        ra.write(ba)
    }

    override fun writeFloat(f: Float) {ra.writeFloat(f) }
}