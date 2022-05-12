package rb.extendo.kiebabo

import rb.vectrix.mathUtil.b

object BitPacking {
    fun packInt( byte0 : Byte, byte1: Byte, byte2: Byte, byte3 : Byte) =
        ((byte0.toUByte().toUInt()) or
                (byte1.toUByte().toUInt() shl 8) or
                (byte2.toUByte().toUInt() shl 16) or
                (byte3.toUByte().toUInt() shl 24)).toInt()

    fun unpackInt(int: Int) : ByteArray {
        val ret = ByteArray(4)
        ret[0] = (int and 0xff).b
        ret[1] = ((int shr 8) and 0xff).b
        ret[2] = ((int shr 16) and 0xff).b
        ret[3] = ((int shr 24) and 0xff).b
        return ret
    }
}