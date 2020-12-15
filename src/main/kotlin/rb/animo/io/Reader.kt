package rb.animo.io

interface IReader {
    fun readInt() : Int
    fun readUShort() : Int
    fun readShort() : Short
    fun readByte() : Int
    fun readUtf8() : String
    fun readFloat() : Float
}

object EndOfStreamException : Exception()