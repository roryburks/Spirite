package spirite.base.graphics.rendering

import spirite.base.brains.ICentralObservatory
import spirite.base.graphics.IFlushable
import spirite.base.graphics.IImage
import spirite.base.graphics.IResourceUseTracker
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.sources.GroupNodeSource
import spirite.base.graphics.rendering.sources.RenderSource
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.IImageWorkspace
import spirite.hybrid.Hybrid

/**
 * The Render Engine has two primary jobs:
 *  1) To centralize the methodology for rendering more complex regular rendering targets with non-standard settings.
 *  2) To cache rendered images that might be used frequently (such as the primary rendered Image).
 */
interface IRenderEngine {
    /** PullImage will create a new image based on the target, not caching it and creating a copy of the cached image
     * if the cached image exists.  The caller of the pullImage is responsible for flushing the image in a timely manner.*/
    fun pullImage( target: RenderTarget) : RawImage

    fun renderImage( target: RenderTarget) : IImage
    fun renderWorkspace( workspace: IImageWorkspace) :IImage =
            renderImage( RenderTarget(GroupNodeSource(workspace.groupTree.root, workspace), RenderSettings(workspace.width, workspace.height)))
}

data class RenderTarget(
        val renderSource: RenderSource,
        val renderSettings: RenderSettings = RenderSettings( renderSource.defaultWidth, renderSource.defaultHeight, true))

data class RenderSettings(
        val width: Int,
        val height: Int,
        val drawSelection: Boolean = true)

class CachedImage(  image: RawImage) : IFlushable {
    val image = image
        get() {
            lastUsed = Hybrid.system.currentMilliseconds
            return field
        }
    var lastUsed = Hybrid.system.currentMilliseconds

    override fun flush() { image.flush() }
}

class RenderEngine(
        val resourceUseTracker: IResourceUseTracker,
        val centralObservatory: ICentralObservatory)
    : IRenderEngine, ImageObserver
{

    init {
        centralObservatory.trackingImageObserver.addObserver(this)
    }

    override fun pullImage(target: RenderTarget): RawImage {
        // Access Cached Image (if it exists)
        val cached = imageCache[target]
        if( cached != null)
            return cached.image.deepCopy()

        // Render Image

        // Lifecycle passed to whatever called this
        val image = Hybrid.imageCreator.createImage(target.renderSettings.width, target.renderSettings.height)
        target.renderSource.render( target.renderSettings, image.graphics)

        return image
    }

    override fun renderImage(target: RenderTarget) : IImage {
        // Access Cached Image (if it exists)
        val cached = imageCache[target]
        if( cached != null)
            return cached.image

        // Render Image

        // Lifecycle handled by the RenderEngine
        val image = Hybrid.imageCreator.createImage(target.renderSettings.width, target.renderSettings.height)
        target.renderSource.render( target.renderSettings, image.graphics)

        // Save to Cache
        imageCache.put(target, CachedImage(image))

        return image
    }

    private val imageCache = mutableMapOf<RenderTarget,CachedImage>()

    override fun imageChanged(evt: ImageChangeEvent) {
        // TODO: Could sort these by handleId and madee into a binsearch if necessary
        imageCache.entries.removeIf { entry ->
            val target = entry.key
            val remove = when {
                evt.workspace != target.renderSource.workspace -> false
                target.renderSource.imageDependencies.any {  evt.handlesChanged.contains(it) } -> true
                target.renderSource.nodeDependencies.any {  evt.nodesChanged.contains(it) } -> true
                else -> false
            }
            if( remove)
                entry.value.flush()

            remove
        }
    }
}
