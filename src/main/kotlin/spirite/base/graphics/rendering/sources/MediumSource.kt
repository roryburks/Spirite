package spirite.base.graphics.rendering.sources

import rb.extendo.dataStructures.SinglyList
import rb.glow.GraphicsContext_old
import rb.glow.IGraphicsContext
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
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

    override fun render(settings: RenderSettings, gc: IGraphicsContext) {
        gc.pushState()
        gc.preTranslate( -medium.x.d, -medium.y.d)
        gc.preScale(settings.width / medium.width.d, settings.height/medium.height.d)
        medium.medium.render(gc)
    }
}