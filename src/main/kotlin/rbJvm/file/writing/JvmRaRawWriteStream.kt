package rbJvm.file.writing

import rb.file.BigEndianWriteStream
import rb.file.IRawWriteStream
import rb.file.IWriteStream
import java.io.RandomAccessFile

class JvmRaRawWriteStream(val ra: RandomAccessFile) : IRawWriteStream {
    override val pointer: Long get() = ra.filePointer

    override fun goto(pointer: Long) {ra.seek(pointer) }

    override fun write(byteArray: ByteArray) { ra.write(byteArray) }

    override fun finish() { }

    override fun close() { ra.close() }
}

fun RandomAccessFile.toBufferedWriteStream() : IWriteStream{

    val raw = JvmRaRawWriteStream(this)
    //val buffered = BufferedWriteStream(raw)
    val out = BigEndianWriteStream(raw)
    return out
}