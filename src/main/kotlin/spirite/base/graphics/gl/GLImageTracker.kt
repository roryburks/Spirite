package spirite.base.graphics.gl

import rb.glow.gl.IGLTexture
import spirite.hybrid.Hybrid
import java.lang.ref.WeakReference

object GLImageTracker {
    private data class ImageData(
            val w: Int, val h: Int, val tex: IGLTexture)

    val images get() = _images.mapNotNull { it.first.get() }
    private val _images = mutableListOf<Pair<WeakReference<GLImage>,ImageData>>()




    val bytesUsed get() = images.fold(0L) { acc, it -> acc + it.width*it.height*4L}

    internal fun glImageLoaded(image: GLImage) {
        _images.add(Pair(WeakReference(image),ImageData(image.width, image.height,image._tex)))
        _checkStatus()
    }

    internal fun glImageUnloaded( image: GLImage) {
        _images.removeIf { it.first.get() == image }
        _checkStatus()
    }

    private fun _checkStatus() {
        _images.removeIf {
            when( it.first.get()) {
                null -> {
                    println("Deleting garbage-collected GL Texture of size: ${it.second.w}xi${it.second.h}")
                    Hybrid.gl.deleteTexture(it.second.tex)
                    true
                }
                else -> false
            }
        }
    }
}