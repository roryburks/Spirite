package rbJvm.file.writing

import rb.file.IRawWriteStream
import java.io.RandomAccessFile

class JvmRaRawWriteStream(val ra: RandomAccessFile) : IRawWriteStream {
    override val pointer: Long get() = ra.filePointer

    override fun goto(pointer: Long) {ra.seek(pointer) }

    override fun write(byteArray: ByteArray) { ra.write(byteArray) }

    override fun finish() { }

    override fun close() { ra.close() }
}