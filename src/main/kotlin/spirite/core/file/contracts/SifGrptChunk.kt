package spirite.core.file.contracts

data class SifGrptChunk(
    val nodes: List<SifGrptNode>)

data class SifGrptNode(
    val settingsBitFlag: Byte,
    val name: String,
    val data: SifGrptNodeData,
    val depth: Int,
    val obs_alpha: Float? = null,
    val obs_xOffset: Int? = null,
    val obs_yOffset: Int? = null)

sealed class SifGrptNodeData {}

object SifGrptNode_Group : SifGrptNodeData() {}

data class SifGrptNode_Simple(
    val mediumId: Int ) : SifGrptNodeData()

data class SifGrptNode_Sprite(
    val layerType: Int,
    val parts: List<Part>) : SifGrptNodeData()
{
    class Part(
        val partTypeName: String,
        val transX: Float,
        val transY: Float,
        val scaleX: Float,
        val scaleY: Float,
        val rotation: Float,
        val drawDepth: Int,
        val mediumId: Int,
        val alpha: Float)
}

data class SifGrptNode_Reference(
    val nodeId: Int ) :SifGrptNodeData()
