package rb.extendo.kiebabo

object BitPacking {
    fun packInt( byte0 : Byte, byte1: Byte, byte2: Byte, byte3 : Byte) =
        ((byte0.toUByte().toUInt()) or
                (byte1.toUByte().toUInt() shl 8) or
                (byte2.toUByte().toUInt() shl 16) or
                (byte3.toUByte().toUInt() shl 24)).toInt()
}