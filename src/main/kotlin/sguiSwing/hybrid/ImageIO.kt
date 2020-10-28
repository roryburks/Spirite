package sguiSwing.hybrid

import rb.glow.img.IImage
import rb.glow.img.RawImage
import rbJvm.glow.awt.ImageBI
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** Converts an IImage to a byteArray in various image formats.
 *
 * In the future it might need to be moved to a generic IOutputStream format.  */
interface IImageIO {
    fun writePNG(image: IImage) : ByteArray
    fun saveImage(image: IImage, file: File)
    fun loadImage( byteArray: ByteArray) : RawImage
}

object JImageIO : IImageIO {
    override fun saveImage(image: IImage, file: File) {
        ImageIO.write((Hybrid.imageConverter.convert(image,ImageBI::class) as ImageBI).bi, file.ext, file)
    }

    override fun writePNG(image: IImage): ByteArray {
        return ByteArrayOutputStream()
                .apply { ImageIO.write((Hybrid.imageConverter.convert(image, ImageBI::class)as ImageBI).bi, "png", this)}
                .toByteArray()
    }

    override fun loadImage(byteArray: ByteArray): RawImage {
        val bi = ImageIO.read(ByteArrayInputStream(byteArray))
        return Hybrid.imageConverter.convertToInternal(ImageBI(bi))
    }
}

val File.ext : String get() {
    val index = name.indexOf('.')
    if( index == -1) return ""
    return name.substring(index+1)
}