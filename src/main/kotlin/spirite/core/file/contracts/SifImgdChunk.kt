package spirite.core.file.contracts

import spirite.core.file.SifFileException

class SifImgdChunk(val mediums: List<SifImgdMedium>)

class SifImgdMedium(
    val id: Int,
    val data: SifImgdMediumData)

sealed class SifImgdMediumData

class SifImgdMed_Plain(val rawImgData: ByteArray) : SifImgdMediumData()
class SifImgdMed_Dynamic(
    val offsetX: Short,
    val offsetY: Short,
    val rawImgData: ByteArray?) : SifImgdMediumData()

object SifImgdMed_Prismatic : SifImgdMediumData()

// Maglev
class SifImgdMed_Maglev(
    val offsetX: Short,
    val offsetY: Short,
    val rawImgData: ByteArray,
    val things: List<SifImgdMagThing>) :SifImgdMediumData()

sealed class SifImgdMagThing

class SifImgdMagThing_Stroke(
    val color: Int,
    val method: Byte,
    val width: Float,
    val drawMode : Byte,
    val xs : FloatArray,
    val ys : FloatArray,
    val ws : FloatArray,
    val preInterpolated: Boolean = true) :SifImgdMagThing()
{
    init {
        if( xs.size != ys.size || xs.size != ws.size)
            throw SifFileException("Mismatching thing size")
    }
}


class SifImgdMagThing_Fill(
    val color: Int,
    val medhod: Byte,
    val refPoints: List<RefPoint> ): SifImgdMagThing()
{
    class RefPoint(
        val strokeRef: Int,
        val startPoint: Int,
        val endPoint: Int)
}
