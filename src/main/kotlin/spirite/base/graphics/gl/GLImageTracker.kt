package spirite.base.graphics.gl

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

object GLImageTracker {
    val images get() = _images.map { it.first.get() }.filterNotNull()

    private val _images = mutableListOf<Pair<WeakReference<GLImage>,IGLTexture>>()




    val bytesUsed get() = images.fold(0L, {acc, it -> acc + it.width*it.height*4L})

    internal fun glImageLoaded(image: GLImage) {
        _images.add(Pair(WeakReference(image),image._tex))
        _checkStatus()
    }

    internal fun glImageUnloaded( image: GLImage) {
        _images.removeIf { it.first.get() == image }
        _checkStatus()
    }

    private fun _checkStatus() {
        _images.removeIf {
            (it.first.get() == null).also { removed ->
                if( removed)
                    println("TODO: Make this hard flush")
            }
        }
    }
}