package spirite.core.file.load.imgd

import rb.file.IReadStream
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.round
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*

class SifMaglevMediumReader(val version: Int) : ISifMediumReader {
    override fun read(read: IReadStream): SifImgdMediumData {
        val numThings = read.readUnsignedShort()
        val things = List(numThings) {
            val thingType = read.readUnsignedByte()
            when( thingType) {
                SifConstants.MAGLEV_THING_STROKE -> {
                    val color = read.readInt()
                    val method = read.readByte()
                    val width = read.readFloat()
                    val mode = read.readByte()

                    val numVertices = read.readInt()
                    val x = read.readFloatArray(numVertices)
                    val y = read.readFloatArray(numVertices)
                    val w = read.readFloatArray(numVertices)
                    SifImgdMagThing_Stroke(color, method, width, mode, x, y, w)
                }
                SifConstants.MAGLEV_THING_FILL -> {
                    val color = read.readInt()
                    val mode = read.readByte()
                    val numSegments = read.readUnsignedShort()
                    val segments = List(numSegments) {
                        val strokeId = read.readInt()
                        val start = read.readInt()
                        val end = read.readInt()
                        SifImgdMagThing_Fill.RefPoint(strokeId, start, end)
                    }
                    SifImgdMagThing_Fill(color, mode, segments)
                }
                else -> throw SifFileException("Unrecognized Thing type: $thingType")
            }

        }

        val imgSize = if( version >= 0x1_0009) read.readInt() else 0
        val oX = if( version >= 0x1_0009) read.readShort() else 0
        val oY = if( version >= 0x1_0009) read.readShort() else 0
        val raw = read.readByteArray(imgSize)

        return  SifImgdMed_Maglev(oX, oY, raw, things)
    }

}

class SifMaglevMediumReader_Legacy_1_0000_To_1_0006(val version: Int) : ISifMediumReader {
    override fun read(read: IReadStream): SifImgdMediumData {
        val numThings = read.readUnsignedShort()
        val things = List(numThings) {
            val thingType = read.readUnsignedByte()
            when(thingType) {
                SifConstants.MAGLEV_THING_STROKE ->{
                    val color = read.readInt()
                    val method = read.readByte()
                    val width = read.readFloat()
                    val mode =
                        if( version < 0x1_0006) SifConstants.PenMode_Normal.toByte()
                        else read.readByte()

                    // xywxywxyw rather than xxxxxxxyyyyyyyywwwwww
                    val numVerts = read.readUnsignedShort()
                    val xyw = read.readFloatArray(numVerts*3)
                    val x = FloatArray(numVerts)
                    val y = FloatArray(numVerts)
                    val w = FloatArray(numVerts)
                    repeat(numVerts) {i ->
                        x[i] = xyw[3*i]
                        y[i] = xyw[3*i+1]
                        w[i] = xyw[3*i+2]
                    }

                    SifImgdMagThing_Stroke( color, method, width, mode,  x, y, w, true )
                }
                SifConstants.MAGLEV_THING_FILL -> throw SifFileException("Maglev Fills should not show up in version x1_0000 - x1_0006")
                else -> throw SifFileException("Unrecognized Thing type in Legacy_pre_1_0000_MaglevMediumLoader: $thingType")
            }
        }

        return  SifImgdMed_Maglev(0, 0, ByteArray(0), things)
    }
}

// NOTE: I'm not super confident in this guy working
class SifMaglevMediumReader_Legacy_Pre_1_0000(val version: Int) : ISifMediumReader {
    override fun read(read: IReadStream) :SifImgdMediumData {
        val numThings = read.readUnsignedShort()
        val strokeLengths = mutableMapOf<Int,Int>()
        val things = List(numThings) {thingId ->
            val thingType = read.readUnsignedByte()
            when(thingType) {
                SifConstants.MAGLEV_THING_STROKE -> {
                    val color = read.readInt()
                    val method = read.readByte()
                    val width = read.readFloat()

                    // One of the primary reasons this is Legacy: reads floats as
                    // xywxywxyw rather than xxxxxxxyyyyyyyywwwwww
                    // also they're not pre-interpolated
                    val numVerts = read.readUnsignedShort()
                    val xyw = read.readFloatArray(numVerts*3)
                    val x = FloatArray(numVerts)
                    val y = FloatArray(numVerts)
                    val w = FloatArray(numVerts)
                    repeat(numVerts) {i ->
                        x[i] = xyw[3*i]
                        y[i] = xyw[3*i+1]
                        w[i] = xyw[3*i+2]
                    }
                    strokeLengths[thingId] = numVerts

                    val drawMode = SifConstants.PenMode_Normal.toByte()
                    SifImgdMagThing_Stroke(color, method, width, drawMode, x, y, w , false)
                }
                SifConstants.MAGLEV_THING_FILL -> {
                    val color = read.readInt()
                    val method = read.readByte()

                    val numSegments = read.readUnsignedShort()
                    val segs = List(numSegments) {
                        val strokeId = read.readUnsignedShort()
                        val startF = read.readFloat()
                        val endF = read.readFloat()

                        // Other primary reason this is legacy is that these start/End f are recorded
                        // as [0,1] but later it's [0,strokeLen] (as the [0,1] requires well-ordering
                        val strokeSize = strokeLengths[strokeId] ?: 0

                        val start = MathUtil.clip ( 0, (strokeSize* startF).round, strokeSize-1)
                        val end = MathUtil.clip ( 0, (strokeSize* endF).round, strokeSize-1)
                        SifImgdMagThing_Fill.RefPoint(strokeId, start, end)
                    }

                    SifImgdMagThing_Fill(color, method,  segs)
                }
                else -> throw SifFileException("Unrecognized Thing type in Legacy_pre_1_0000_MaglevMediumLoader: $thingType")
            }
        }
        return SifImgdMed_Maglev( 0, 0, ByteArray(0), things)
    }
}