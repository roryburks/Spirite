package spirite.base.graphics.gl

import spirite.base.graphics.IImageTracker

class GLTrack() : IImageTracker{
    internal fun registerImage( image: GLImage) {
        _imagesLoaded.add(image)
    }
    internal fun relinquishImage( image: GLImage) {
        _imagesLoaded.remove(image)
    }

    val imagesLoaded : List<GLImage> get() = _imagesLoaded
    private val _imagesLoaded = mutableListOf<GLImage>()

    override val bytesUsed : Int
        get() = _imagesLoaded.sumBy { it.width*it.height*4 }
}