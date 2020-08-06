package sguiSwing.hybrid.Transferables

import rb.glow.IImage
import rbJvm.glow.awt.ImageBI
import sguiSwing.hybrid.Hybrid
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


val IImageDataFlavor = DataFlavor(IImage::class.java, "SpiriteInternalImage")

class TransferableImage(image: IImage) : Transferable {
    val image = image.deepCopy()
    val bImage = (Hybrid.imageConverter.convert(this.image,ImageBI::class) as ImageBI).bi

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