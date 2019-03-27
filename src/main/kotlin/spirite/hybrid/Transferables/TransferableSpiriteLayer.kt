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

class TransferableSpiriteLayer(layer: Layer) : Transferable {
    private val _layer = layer

    fun buildLayer( workspace: MImageWorkspace) = _layer.dupe(workspace)

    override fun getTransferData(flavor: DataFlavor?): Any {
        when( flavor) {
            SpiriteLayerDataFlavor -> return {workspace : MImageWorkspace -> buildLayer(workspace)}
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = dataFlavors

    val dataFlavors = arrayOf( IImageDataFlavor, DataFlavor.imageFlavor)
}