package spirite.base.graphics.rendering.sources

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.util.f
import spirite.base.util.groupExtensions.SinglyList

data class MediumSource(val medium: MediumHandle, override val workspace: IImageWorkspace) : RenderSource {
    override val defaultWidth: Int get() = medium.width
    override val defaultHeight: Int get() = medium.height
    override val imageDependencies: Collection<MediumHandle> get() = SinglyList(medium)
    override val nodeDependencies: Collection<Node> get() = emptySet()
    override val rendersLifted: Boolean get() = false

    override fun render(settings: RenderSettings, gc: GraphicsContext) {
        gc.pushState()
        gc.preTranslate( -medium.x.f, -medium.y.f)
        gc.preScale(settings.width.f / medium.width.f, settings.height.f/medium.height.f)
        medium.medium.render(gc)
    }
}