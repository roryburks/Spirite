package spirite.base.graphics

import spirite.base.graphics.gl.GLImage
import spirite.base.graphics.gl.GLImageTracker

interface IResourceUseTracker {
    val bytesUsed: Long
}

class ResourceUseTracker : IResourceUseTracker {
    override val bytesUsed: Long
        get() = TODO("not implemented")

}

interface IDetailedResourceUseTracker : IResourceUseTracker{
    val glImages: Set<GLImage>

}
class DetailedResourceUseTracker : IDetailedResourceUseTracker {
    override val bytesUsed: Long get() = GLImageTracker.bytesUsed

    override val glImages: Set<GLImage> get() = GLImageTracker.images
}