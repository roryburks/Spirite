package spirite.base.graphics.gl

object GLImageTracker {
    val images : Set<GLImage> get() = _images
    private val _images = mutableSetOf<GLImage>()

    val bytesUsed get() = images.fold(0L, {acc, it -> acc + it.width*it.height*4L})

    internal fun glImageLoaded(image: GLImage) {
        _images.add(image)
    }

    internal fun glImageUnloaded( image: GLImage) {
        _images.remove(image)
    }
}