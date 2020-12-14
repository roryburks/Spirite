package rb.glow.img

import rb.glow.Colors
import rb.glow.IFlushable
import rb.glow.IGraphicsContext


/**
 * RawImage is a wrapper for multiple different types of more-native image formats.
 */

interface RawImage : IImage, IFlushable {
    /** Gets the GraphicsContext for writing to the image.  */
    val graphics: IGraphicsContext

    /** Flushes the image, marking it as no longer being used, allowing it to
     * free up underlying resources.
     *
     * NOTE: it is not guaranteed that flush will ever be called, so if the
     * image is using native resources that need to be de-allocated, be sure
     * to override finalize.
     */
    override fun flush()

    class InvalidImageDimensionsExeption(message: String) : Exception(message)
}

object NillImage: RawImage {
    override val graphics: IGraphicsContext get() = throw Exception("Can't draw on Nill Image")
    override val width: Int get() = 1
    override val height: Int get() = 1
    override val byteSize: Int get() = 1

    override fun flush() {}
    override fun deepCopy() : RawImage { return this}
    override fun getARGB(x: Int, y: Int) = 0
    override fun getColor(x: Int, y: Int) = Colors.TRANSPARENT
}