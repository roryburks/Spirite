package spirite.core.file.contracts

class SifAnimChunk(
    val animations: List<SifAnimAnimation>)

class SifAnimAnimation(
    val name: String,
    val speed: Float,
    val zoom: Short,
    val data: SifAnimAnimData)

sealed class SifAnimAnimData

// FFA
class SifAnimAnim_FixedFrame(
) : SifAnimAnimData()
class SifAnimFfaLayer(
    val partTypeName: String,
    val isAsync : Boolean,
    val data : SifAnimFfaLayerData )

sealed class SifAnimFfaLayerData
class SifAnimFfaLayer_Grouped(
    val groupNodeId: Int,
    val subgroupsLinked: Boolean,
    val frames: List<Frames> )
{
    class Frames(
        val type : Byte,
        val nodeId: Int,
        val len: Int)
}
class SifAnimFfaLayer_Lexical(
    val groupedNodeId: Int,
    val lexicon: String,
    val explicitMapping: List<Pair<Char,Int>> )
class SifAnimFfaLayer_Cascading(
    val groupedNodeId: Int,
    val lexicon: String,
    val sublayers: List<SubLayer> )
{
    class SubLayer(
        val nodeId: Int,
        val primaryLen: Int,
        val lexicalKey: Char,
        val lexicon: String )
}

// Rig
object SifAnimAnim_Rig : SifAnimAnimData()