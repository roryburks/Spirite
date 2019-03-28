package spirite.hybrid.Transferables

import spirite.base.graphics.IImage
import spirite.base.imageData.layers.Layer
import spirite.hybrid.Hybrid
import spirite.hybrid.Transferables.IClipboard.ClipboardThings
import spirite.hybrid.Transferables.IClipboard.ClipboardThings.Image
import spirite.pc.graphics.ImageBI
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage

interface IClipboard
{
    enum class ClipboardThings
    {
        Image,
        Layer
    }

    fun postToClipboard( any: Any)
    fun getFromClipboard(things: Set<ClipboardThings>? = null) : Any?
}

object SwClipboard : IClipboard
{
    override fun postToClipboard(any: Any) {
        val transferable = when(any) {
            is Layer ->  TransferableSpiriteLayer(any)
            is IImage -> TransferableImage(any)
            else -> throw NotImplementedError("Don't know how to convert object into a Transferable")
        }

        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    override fun getFromClipboard(things: Set<ClipboardThings>?): Any? {
        val clip = Toolkit.getDefaultToolkit().systemClipboard

        if( things?.contains(Image) ?: true) {
            if( clip.isDataFlavorAvailable( IImageDataFlavor)) {
                return clip.getData(IImageDataFlavor)
            }
            if( clip.isDataFlavorAvailable( DataFlavor.imageFlavor)) {
                val image = (clip.getData(DataFlavor.imageFlavor) as java.awt.Image)
                if( image is BufferedImage)
                    return Hybrid.imageConverter.convertToInternal(ImageBI(image))
                else {
                    val bi = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB)
                    bi.graphics.drawImage(image, 0, 0, null)
                    return Hybrid.imageConverter.convertToInternal(ImageBI(bi))
                }
            }
        }
        if( things?.contains(ClipboardThings.Layer) ?: true) {
            if( clip.isDataFlavorAvailable(SpiriteLayerDataFlavor)) return clip.getData(SpiriteLayerDataFlavor)
        }

        return null
    }
}