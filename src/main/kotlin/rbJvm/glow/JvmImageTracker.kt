package rbJvm.glow

import rb.glow.gl.GLImage
import rb.glow.gl.IGL
import rb.glow.gl.IGLImageTracker
import rb.glow.gl.IGLTexture
import java.lang.ref.WeakReference

object JvmImageTracker : IGLImageTracker{
    private data class ImageData(
        val w: Int, val h: Int, val tex: IGLTexture, val gl: IGL)

    override val images get() = _images.mapNotNull { it.first.get() }
    private val _images = mutableListOf<Pair<WeakReference<GLImage>, ImageData>>()




    override val bytesUsed get() = images.fold(0L) { acc, it -> acc + it.width*it.height*4L}

    override fun markGlImageLoaded(image: GLImage) {
        _images.add(Pair(WeakReference(image), ImageData(image.width, image.height, image._tex, image.engine.gl)))
        _checkStatus()
    }

    override fun markGLImageUnloaded(image: GLImage) {
        _images.removeIf { it.first.get() == image }
        _checkStatus()
    }

    private fun _checkStatus() {
        _images.removeIf {(ref,data) ->
            when( ref.get()) {
                null -> {
                    println("Deleting garbage-collected GL Texture of size: ${data.w}xi${data.h}")
                    data.gl.deleteTexture(data.tex)
                    true
                }
                else -> false
            }
        }
    }
}