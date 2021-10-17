package spirite.sguiHybrid.transferables

import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.layers.Layer
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


val SpiriteLayerDataFlavor = DataFlavor(Layer::class.java, "SpiriteInternalImage")

data class TransferableLayerData(
    val originalLayerName: String,
    val layer: Layer )
{
    constructor(node: LayerNode):this(node.name, node.layer)
}

interface ILayerBuilder {
    fun buildLayer(workspace: MImageWorkspace) : Layer
    val width: Int
    val height: Int
    val name: String
}

class TransferableSpiriteLayer(layer: Layer, name: String) : Transferable {
    private val _layer = layer
    private val _origLayerName = name

    private val _layerBuilder = object  : ILayerBuilder {
        override fun buildLayer(workspace: MImageWorkspace) = _layer.dupe(workspace)
        override val height: Int get() = _layer.height
        override val width: Int get() = _layer.width
        override val name: String get() = _origLayerName
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        return when( flavor) {
            SpiriteLayerDataFlavor -> _layerBuilder
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = _dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = _dataFlavors

    private val _dataFlavors = arrayOf( SpiriteLayerDataFlavor)
}