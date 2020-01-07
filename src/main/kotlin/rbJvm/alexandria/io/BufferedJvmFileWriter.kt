package rbJvm.alexandria.io

import rb.alexandria.io.IRawWriteStream
import rb.alexandria.io.IWriteStream
import java.io.RandomAccessFile


class BufferedJvmFileWriter(
        val ra: RandomAccessFile,
        val buffSize: Int = 2048) : IRawWriteStream
{
    val channel = ra.channel

    override val pointer: Long get() = channel.position()

    override fun goto(pointer: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(byteArray: ByteArray, start: Int, len: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun finish() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}