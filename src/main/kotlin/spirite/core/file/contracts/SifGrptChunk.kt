package spirite.core.file.contracts

data class SifGrptChunk(
    val nodes: List<SifGrptNode<*>>)

data class SifGrptNode<T : SifGrptNodeData>(
    val settingsBitFlag: Byte,
    val name: String,
    val data: T,
    val depth: Int,
    val obs_alpha: Float,
    val obs_xOffset: Int,
    val obs_yOffset: Int)

sealed class SifGrptNodeData {}

object SifGrptGroupNode : SifGrptNodeData() {}

data class SifGrptSimpleLayerNode(
    val mediumId: Int ) : SifGrptNodeData()

data class SifGrptSpriteLayerNode(
    val layerType: Int,
    val parts: List<SifGrptSpriteLayerPartK>)
data class SifGrptSpriteLayerPartK(
    val partTypeName: String,
    val transX: Float,
    val transY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val drawDepth: Int,
    val mediumId: Int )

data class SifGrptReferenceLayerNodeK(
    val nodeId: Int )
