package rbJvm.animo

import rb.animo.io.IReader
import rb.vectrix.mathUtil.i
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.RandomAccessFile

class JvmReader(val ra: RandomAccessFile)  : IReader {
    override fun readInt() = ra.readInt()
    override fun readUShort() = ra.readUnsignedShort()
    override fun readShort() = ra.readShort()
    override fun readByte() = ra.readUnsignedByte()
    override fun readFloat() = ra.readFloat()

    override fun readUtf8(): String {
        val bos = ByteArrayOutputStream()
        var b = ra.readByte()
        while( b != 0x00.toByte()) {
            bos.write(b.i)
            b = ra.readByte()
        }

        return bos.toString("UTF-8")
    }
}

class JvmDataInputStreamReader( val dis: DataInputStream) : IReader {
    override fun readInt() = dis.readInt()
    override fun readUShort() = dis.readUnsignedShort()
    override fun readShort() = dis.readShort()
    override fun readByte() = dis.readUnsignedByte()
    override fun readFloat() = dis.readFloat()

    override fun readUtf8(): String {
        val bos = ByteArrayOutputStream()
        var b = dis.readByte()
        while( b != 0x00.toByte()) {
            bos.write(b.i)
            b = dis.readByte()
        }

        return bos.toString("UTF-8")
    }
}