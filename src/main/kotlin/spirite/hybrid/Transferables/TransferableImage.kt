package spirite.hybrid.Transferables

import spirite.base.graphics.IImage
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


val IImageDataFlavor = DataFlavor(IImage::class.java, "SpiriteInternalImage")

class TransferableImage(image: IImage) : Transferable {
    val image = image.deepCopy()
    val bImage = Hybrid.imageConverter.convert<ImageBI>(this.image).bi

    override fun getTransferData(flavor: DataFlavor?): Any {
        return when( flavor) {
            DataFlavor.imageFlavor -> bImage
            IImageDataFlavor -> image
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = dataFlavors

    val dataFlavors = arrayOf( IImageDataFlavor, DataFlavor.imageFlavor)
}