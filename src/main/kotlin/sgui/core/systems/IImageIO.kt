package sgui.core.systems

import rb.glow.img.IImage
import rb.glow.img.RawImage
import java.io.File

/** Converts an IImage to a byteArray in various image formats.
 *
 * In the future it might need to be moved to a generic IOutputStream format.  */
interface IImageIO {
    fun writePNG(image: IImage) : ByteArray
    fun saveImage(image: IImage, file: File)
    fun loadImage( byteArray: ByteArray) : RawImage
}