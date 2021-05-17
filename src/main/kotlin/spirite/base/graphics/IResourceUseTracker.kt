package spirite.base.graphics

import rb.glow.gl.GLImage
import sgui.swing.hybrid.Hybrid

interface IResourceUseTracker {
    val bytesUsed: Long
}

class ResourceUseTracker : IResourceUseTracker {
    override val bytesUsed: Long
        get() = TODO("not implemented")

}

interface IDetailedResourceUseTracker : IResourceUseTracker{
    val glImages: Collection<GLImage>

}
class DetailedResourceUseTracker : IDetailedResourceUseTracker {
    override val bytesUsed: Long get() = Hybrid.gl.tracker?.bytesUsed ?: 0

    override val glImages: Collection<GLImage> get() = Hybrid.gl.tracker?.images ?: emptySet()
}