package spirite.base.graphics.rendering

import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.brains.WorkspaceSet
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.graphics.IImageTracker
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.sources.RenderSource
import spirite.base.imageData.IImageWorkspace
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
        val workspaceSet: WorkspaceSet) :
        IRenderEngine, WorkspaceObserver {

    init {
        workspaceSet.workspaceObserver.addObserver(this)
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


    // region WorkspaceObserver
    override fun workspaceCreated(newWorkspace: IImageWorkspace) {}
    override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun workspaceChanged(selectedWorkspace: IImageWorkspace, previousSelected: IImageWorkspace) {}
    // endregion
}
