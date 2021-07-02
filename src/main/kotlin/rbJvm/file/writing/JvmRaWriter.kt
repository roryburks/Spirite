//package rbJvm.file.writing
//
//import rb.file.IFileWriter
//import java.io.RandomAccessFile
//import java.nio.ByteBuffer
//
//class JvmRaWriter(val ra: RandomAccessFile): IFileWriter {
//    override fun writeInt(i: Int) { ra.writeInt(i)}
//    override fun writeShort(i: Int) { ra.writeShort(i) }
//    override fun writeByte(byte: Int) { ra.writeByte(byte) }
//    override fun writeFloat(float: Float) { ra.writeFloat(float) }
//    override fun writeBytes(bytes: ByteArray) { ra.write(bytes) }
//    override fun writeFloats(data: FloatArray) {
//        val buf = ByteBuffer.allocate(data.size * 4)
//        buf.clear()
//        buf.asFloatBuffer().put(data)
//        this.writeBytes(buf.array())
//    }
//}