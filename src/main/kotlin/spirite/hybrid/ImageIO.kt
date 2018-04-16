package spirite.hybrid

import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.hybrid.Transferables.IImageDataFlavor
import spirite.hybrid.Transferables.TransferableImage
import spirite.pc.graphics.ImageBI
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** Converts an IImage to a byteArray in various image formats.
 *
 * In the future it might need to be moved to a generic IOutputStream format.  */
interface IImageIO {
    fun writePNG(image: IImage) : ByteArray
    fun saveImage( image: IImage, file: File)
    fun loadImage( byteArray: ByteArray) : RawImage
    fun imageToClipboard(image: IImage)
    fun imageFromClipboard() : RawImage?
}

object JImageIO : IImageIO {
    override fun saveImage(image: IImage, file: File) {
        ImageIO.write((Hybrid.imageConverter.convert<ImageBI>(image)).bi, file.ext, file)
    }

    override fun writePNG(image: IImage): ByteArray {
        return ByteArrayOutputStream()
                .apply { ImageIO.write(Hybrid.imageConverter.convert<ImageBI>(image).bi, "png", this)}
                .toByteArray()
    }

    override fun loadImage(byteArray: ByteArray): RawImage {
        val bi = ImageIO.read(ByteArrayInputStream(byteArray))
        val img = Hybrid.imageCreator.createImage(bi.width, bi.height)
        val gc = img.graphics
        gc.clear()
        gc.renderImage(ImageBI(bi), 0, 0)
        gc.dispose()
        return img
    }

    override fun imageToClipboard(image: IImage) {
        val transfer = TransferableImage( image)

        Toolkit.getDefaultToolkit().systemClipboard.setContents(transfer, null)
    }

    override fun imageFromClipboard(): RawImage? {
        val clip = Toolkit.getDefaultToolkit().systemClipboard
        if( clip.isDataFlavorAvailable( IImageDataFlavor)) {
            return (clip.getData(IImageDataFlavor) as IImage).deepCopy()
        }
        if( clip.isDataFlavorAvailable( DataFlavor.imageFlavor)) {
            val image = (clip.getData(DataFlavor.imageFlavor) as Image)
            if( image is BufferedImage)
                return Hybrid.imageConverter.convertToInternal(ImageBI(image))
            else {
                val bi = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB)
                bi.graphics.drawImage(image, 0, 0, null)
                return Hybrid.imageConverter.convertToInternal(ImageBI(bi))
            }
        }
        return null
    }
}

val File.ext : String get() {
    val index = name.indexOf('.')
    if( index == -1) return ""
    return name.substring(index+1)
}