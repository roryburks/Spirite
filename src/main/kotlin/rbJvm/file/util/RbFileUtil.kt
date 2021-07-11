package rbJvm.file.util

import rb.file.BigEndianWriteStream
import rb.file.BufferedReadStream
import rb.file.BufferedWriteStream
import rb.file.IRawWriteStream
import rbJvm.file.JvmRandomAccessFileBinaryReadStream
import rbJvm.file.writing.JvmRaRawWriteStream
import java.io.File
import java.io.RandomAccessFile

fun File.toBufferedWrite() : BufferedWriteStream {
    val ra = RandomAccessFile(this, "rw")
    return BufferedWriteStream( JvmRaRawWriteStream(ra) )
}

fun File.toBufferedRead() : BufferedReadStream {
    val ra = RandomAccessFile(this, "r")
    return BufferedReadStream(JvmRandomAccessFileBinaryReadStream(ra))
}

fun IRawWriteStream.toWrite(bigEndian: Boolean = true) =
    if(bigEndian) BigEndianWriteStream(this)
    else throw NotImplementedError()
