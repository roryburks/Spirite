package rb.animo.io


interface IWriter {
    fun writeInt(i : Int)
    fun writeShort( i : Int)
    fun writeByte( i : Int)
    fun writeUtf8( s : String)
    fun writeFloat(f : Float)
}
