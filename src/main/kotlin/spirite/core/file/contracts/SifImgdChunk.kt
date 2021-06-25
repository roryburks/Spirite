package spirite.core.file.contracts

class SifImgdChunk(val mediums: List<SifImgdMed>)

sealed class SifImgdMed

class SifImgdMed_Plain(val rawImgData: ByteArray) : SifImgdMed()
class SifImgdMed_Dynamic(
    val offsetX: Short,
    val offsetY: Short,
    val rawImgData: ByteArray) : SifImgdMed()

object SifImgdMed_Prismatic : SifImgdMed()

// Maglev
class SifImgdMed_Maglev(
    val offsetX: Short,
    val offsetY: Short,
    val rawImgData: ByteArray,
    val things: List<SifImgdMagThing>)

sealed class SifImgdMagThing

class SifImgdMagThing_Stroke(
    val color: Int,
    val method: Byte,
    val width: Float,
    val xs : FloatArray,
    val ys : FloatArray,
    val ps : FloatArray)

class SifImgdMagThing_Fill(
    val color: Int,
    val medhod: Byte,
    val refPoints: List<RefPoint> )
{
    class RefPoint(
        val strokeRef: Int,
        val startPoint: Int,
        val endPoint: Int)
}
