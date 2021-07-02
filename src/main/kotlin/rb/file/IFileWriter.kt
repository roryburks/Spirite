//package rb.file
//
//import java.nio.charset.Charset
//
//interface IFileWriter {
//    fun writeInt(i : Int)
//    fun writeShort( i : Int)
//    fun writeByte( byte: Int)
//    fun writeFloat( float: Float)
//    fun writeBytes( bytes: ByteArray)
//    fun writeFloats(data: FloatArray)
//}
//
//fun IFileWriter.writeUtf8(string: String) {
//    val b = (string + 0.toChar()).toByteArray(Charset.forName("UTF-8"))
//
//    // Convert non-terminating null characters to whitespace
//    val nil: Byte = 0
//    for (i in 0 until b.size - 1) {
//        if (b[i] == nil)
//            b[i] = 0x20
//    }
//
//    this.writeBytes(b)
//}