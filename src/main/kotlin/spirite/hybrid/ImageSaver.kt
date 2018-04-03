package spirite.hybrid

import spirite.base.graphics.IImage
import spirite.pc.graphics.ImageBI
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Converts an IImage to a byteArray in various image formats.
 *
 * In the future it might need to be moved to a generic IOutputStream format.  */
interface IImageSaver {
    fun writePNG(image: IImage) : ByteArray
}

object JImageSaver : IImageSaver {
    override fun writePNG(image: IImage): ByteArray {
        return ByteArrayOutputStream()
                .apply { ImageIO.write(Hybrid.imageConverter.convert<ImageBI>(image).bi, "png", this)}
                .toByteArray()
    }

}