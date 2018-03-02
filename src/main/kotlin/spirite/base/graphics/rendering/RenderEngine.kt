package spirite.base.graphics.rendering

import spirite.base.brains.ICentralObservatory
import spirite.base.graphics.IImage
import spirite.base.graphics.IImageTracker
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.sources.RenderSource
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.hybrid.Hybrid

/**
 * The Render Engine has two primary jobs:
 *  1) To centralize the methodology for rendering more complex regular rendering targets with non-standard settings.
 *  2) To cache rendered images that might be used frequently (such as the primary rendered Image).
 */
interface IRenderEngine {
    fun renderImage( target: RenderTarget, cache: Boolean = false) : IImage
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

class RenderEngine(
        val imageTracker: IImageTracker,
        val centralObservatory: ICentralObservatory) :
        IRenderEngine, ImageObserver
{

    init {
        centralObservatory.trackingImageObserver.addObserver(this)
    }

    override fun renderImage(target: RenderTarget, cache: Boolean) : IImage {
        // Access Cached Image (if it exists)
        val cached = imageCache[target]
        if( cached != null)
            return cached.image

        // Render Image
        val image = Hybrid.imageCreator.createImage(target.renderSettings.width, target.renderSettings.height)
        target.renderSource.render( target.renderSettings, image.graphics)

        // Save to Cache (if told to)
        if( cache)
            imageCache.put(target, CachedImage(image))

        return image
    }

    private val imageCache = mutableMapOf<RenderTarget,CachedImage>()

    override fun imageChanged(evt: ImageChangeEvent) {
        // TODO: Could sort these by handleId and madee into a binsearch if necessary
        imageCache.entries.removeIf { entry ->
            val target = entry.key
            when {
                evt.workspace != target.renderSource.workspace -> false
                target.renderSource.imageDependencies.any {  evt.handlesChanged.contains(it) } -> true
                target.renderSource.nodeDependencies.any {  evt.nodesChanged.contains(it) } -> true
                else -> false
            }
        }
    }
}
