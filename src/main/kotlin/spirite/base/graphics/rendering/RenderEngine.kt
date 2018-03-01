package spirite.base.graphics.rendering

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImageTracker
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.sources.RenderSource
import spirite.hybrid.Hybrid

/**
 * The Render Engine has two primary jobs:
 *  1) To centralize the methodology for rendering more complex regular rendering targets with non-standard settings.
 *  2) To cache rendered images that might be used frequently (such as the primary rendered Image).
 */
interface IRenderEngine {
    fun renderImage( settings: RenderSettings, gc: GraphicsContext, cache: Boolean)
}

data class RenderTarget(
        val renderSource: RenderSource,
        val renderSettings: RenderSettings = RenderSettings( renderSource.defaultWidth, renderSource.defaultHeight, true))

data class RenderSettings(
        val width: Int,
        val height: Int,
        val drawSelection: Boolean = true)

class CachedImage(  image: RawImage) {
    val image = image
        get() {
            lastUsed = Hybrid.system.currentMilliseconds
            return field
        }
    var lastUsed = Hybrid.system.currentMilliseconds

    fun flush() { image.flush() }
}

class RenderEngine( val imageTracker: IImageTracker) : IRenderEngine
{
    override fun renderImage(settings: RenderSettings, gc: GraphicsContext, cache: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val imageCache = mapOf<RenderSettings,CachedImage>()

}
