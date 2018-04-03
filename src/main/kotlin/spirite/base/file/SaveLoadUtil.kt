package spirite.base.file

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

object SaveLoadUtil {
    val header : ByteArray get() =ByteArray(4).apply {
        System.arraycopy("SIFF".toByteArray(charset("UTF-8")), 0, this, 0, 4)
    }
    val version = 0x0001_0001


    // :::: Node Type Identifiers for the SIFF GroupTree Section
    val NODE_GROUP = 0x00
    val NODE_SIMPLE_LAYER = 0x01
    val NODE_RIG_LAYER = 0x02
    val NODE_REFERENCE_LAYER = 0x03
    val NODE_PUPPET_LAYER = 0x04

    // :::: MediumType
    val MEDIUM_PLAIN = 0x00
    val MEDIUM_DYNAMIC = 0x01
    val MEDIUM_PRISMATIC = 0x02
    val MEDIUM_MAGLEV = 0x03

    // Node Attribute Masks
    val VISIBLE_MASK = 0x01
    val EXPANDED_MASK = 0x02

    fun strToByteArrayUTF8( str: String) : ByteArray
    {
        val b = (str + 0.toChar()).toByteArray(Charset.forName("UTF-8"))

        // Convert non-terminating null characters to whitespace
        val nil : Byte = 0
        for( i in 0 until b.size-1) {
            if( b[i] == nil)
                b[i] = 0x20
        }

        return b
    }
}