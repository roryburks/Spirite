package sgui.swing.systems

import rb.glow.IImageConverter
import rb.glow.img.IImage
import rb.glow.img.RawImage
import rbJvm.glow.awt.ImageBI
import sgui.core.systems.IImageIO
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class JImageIO(private val _imageConverter : IImageConverter) : IImageIO {
    override fun saveImage(image: IImage, file: File) {
        ImageIO.write((_imageConverter.convert(image,ImageBI::class) as ImageBI).bi, file.ext, file)
    }

    override fun writePNG(image: IImage): ByteArray {
        return ByteArrayOutputStream()
                .apply { ImageIO.write((_imageConverter.convert(image, ImageBI::class)as ImageBI).bi, "png", this)}
                .toByteArray()
    }

    override fun loadImage(byteArray: ByteArray): RawImage {
        val bi = ImageIO.read(ByteArrayInputStream(byteArray))
        return _imageConverter.convertToInternal(ImageBI(bi))
    }
}

val File.ext : String get() {
    val index = name.indexOf('.')
    if( index == -1) return ""
    return name.substring(index+1)
}