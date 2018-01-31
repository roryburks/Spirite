package spirite.base.graphics


/**
 * RawImage is a wrapper for multiple different types of more-native image formats.
 */

interface RawImage : IImage {
    /** Gets the GraphicsContext for writing to the image.  */
    val graphics: GraphicsContext

    class InvalidImageDimensionsExeption(message: String) : Exception(message)
}
