package rb.glow.gl

interface IGLImageTracker {
    val bytesUsed: Long
    val images: List<GLImage>

    fun markGlImageLoaded( image: GLImage)
    fun markGLImageUnloaded(image: GLImage)
}