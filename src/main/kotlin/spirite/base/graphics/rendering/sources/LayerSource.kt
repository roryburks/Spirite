package spirite.base.graphics.rendering.sources

import rb.glow.GraphicsContext_old
import rb.glow.IGraphicsContext
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.Layer

data class LayerSource(val layer: Layer, override val workspace: IImageWorkspace) : RenderSource {
    override val defaultWidth: Int get() = layer.width
    override val defaultHeight: Int get() = layer.height
    override val imageDependencies: Collection<MediumHandle> get() = layer.imageDependencies
    override val nodeDependencies: Collection<Node> get() = emptySet()
    override val rendersLifted: Boolean get() = false

    override fun render(settings: RenderSettings, gc: IGraphicsContext) {
        gc.pushState()
        gc.preTranslate( -layer.x.d, -layer.y.d)
        gc.preScale(settings.width / layer.width.d, settings.height/layer.height.d)
        layer.getDrawList()
                .sortedBy { it.drawDepth }
                .forEach { it.handle.medium.render(gc.old, it.renderRubric) }
    }
}