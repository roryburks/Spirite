package spirite.base.graphics.rendering.sources

import rb.extendo.dataStructures.SinglyList
import rb.vectrix.mathUtil.f
import rb.glow.GraphicsContext
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.Node

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