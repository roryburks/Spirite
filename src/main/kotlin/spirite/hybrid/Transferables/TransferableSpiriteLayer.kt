package spirite.hybrid.Transferables

import spirite.base.graphics.IImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


val SpiriteLayerDataFlavor = DataFlavor(IImage::class.java, "SpiriteInternalImage")

typealias LayerBuilder = (MImageWorkspace) -> Layer
class TransferableSpiriteLayer(layer: Layer) : Transferable {
    private val _layer = layer

    fun buildLayer( workspace: MImageWorkspace) = _layer.dupe(workspace)
    var layerBuilder : LayerBuilder = {workspace : MImageWorkspace -> buildLayer(workspace)}

    override fun getTransferData(flavor: DataFlavor?): Any {
        return when( flavor) {
            SpiriteLayerDataFlavor -> layerBuilder
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = dataFlavors

    val dataFlavors = arrayOf( IImageDataFlavor, DataFlavor.imageFlavor)
}