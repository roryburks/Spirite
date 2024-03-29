package spirite.base.file.sif

import rb.vectrix.mathUtil.i
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset

object SaveLoadUtil {
    val header : ByteArray get() = byteArrayOf(0x53, 0x49, 0x46, 0x46) // "SIFF"
//        ByteArray(4).apply {
//        System.arraycopy("SIFF".toByteArray(charset("UTF-8")), 0, this, 0, 4)
//    }
    const val version = 0x0001_0010


    // :::: GroupNode Type Identifiers for the SIFF GroupTree Section
    const val NODE_GROUP = 0x00
    const val NODE_SIMPLE_LAYER = 0x01
    const val NODE_SPRITE_LAYER = 0x02
    const val NODE_REFERENCE_LAYER = 0x03
    const val NODE_PUPPET_LAYER = 0x04

    // :::: MediumType
    const val MEDIUM_PLAIN = 0x00
    const val MEDIUM_DYNAMIC = 0x01
    const val MEDIUM_PRISMATIC = 0x02
    const val MEDIUM_MAGLEV = 0x03

    // :::: Maglev Thing Type
    const val MAGLEV_THING_STROKE = 0
    const val MAGLEV_THING_FILL = 1

    // :::: AnimationType
    const val ANIM_FFA = 0x01
    const val ANIM_RIG = 0x02

    // :::: AnimationSpaceType
    const val ANIMSPACE_FFA = 0x01

    // :::: FFAFrameType
    const val FFAFRAME_FRAME = 0x01
    const val FFAFRAME_STARTOFLOOP = 0x02
    const val FFAFRAME_GAP = 0x03
    const val FFAFRAME_ENDOFLOOP = 0x04

    // :::: FFALayerType
    const val FFALAYER_GROUPLINKED = 0x01
    const val FFALAYER_LEXICAL = 0x02
    const val FFALAYER_CASCADING = 0x03

    // GroupNode Attribute Masks
    const val VisibleMask = 0b1
    const val ExpandedMask = 0b10
    const val FlattenedMask = 0b100

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

    fun readNullTerminatedStringUTF8(ra: RandomAccessFile) : String
    {
        val bos = ByteArrayOutputStream()
        var b = ra.readByte()
        while( b != 0x00.toByte()) {
            bos.write(b.i)
            b = ra.readByte()
        }

        return bos.toString("UTF-8")
    }
}

