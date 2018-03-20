package spirite.base.graphics

import spirite.base.util.Colors


/**
 * RawImage is a wrapper for multiple different types of more-native image formats.
 */

interface RawImage : IImage {
    /** Gets the GraphicsContext for writing to the image.  */
    val graphics: GraphicsContext

    class InvalidImageDimensionsExeption(message: String) : Exception(message)
}

object NillImage: RawImage {
    override val graphics: GraphicsContext get() = throw Exception("Can't draw to Nill Image")
    override val width: Int get() = 1
    override val height: Int get() = 1
    override val byteSize: Int get() = 1

    override fun flush() {}
    override fun deepCopy() :RawImage { return this}
    override fun getARGB(x: Int, y: Int) = 0
    override fun getColor(x: Int, y: Int) = Colors.TRANSPARENT
}