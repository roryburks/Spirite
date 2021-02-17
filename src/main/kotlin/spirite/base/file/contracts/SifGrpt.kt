package spirite.base.file.contracts

data class SifFileGroupTreeChunkK(
        val nodes: List<SifGrptNodeK<*>>)

data class SifGrptNodeK<T : SifGrptNodeDataK>(
        val depth: Int,
        val alpha: Float,
        val xOffset: Int,
        val yOffset: Int,
        val settingsBitFlag: Byte,
        val name: String,
        val data: T)

sealed class SifGrptNodeDataK {}

object SifGrptGroupNode : SifGrptNodeDataK() {}

data class SifGrptSimpleLayerNode(
        val mediumId: Int ) : SifGrptNodeDataK()

data class SifGrptSpriteLayerNode(
        val layerType: Int)
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