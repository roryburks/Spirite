package spirite.base.graphics.rendering.sources

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.Layer
import spirite.base.util.f

data class LayerSource(val layer: Layer, override val workspace: IImageWorkspace) : RenderSource {
    override val defaultWidth: Int get() = layer.width
    override val defaultHeight: Int get() = layer.height
    override val imageDependencies: Collection<MediumHandle> get() = layer.imageDependencies
    override val nodeDependencies: Collection<Node> get() = emptySet()

    override fun render(settings: RenderSettings, gc: GraphicsContext) {
        gc.pushState()
        gc.preTranslate( -layer.x.f, -layer.y.f)
        layer.getDrawList()
                .sortedBy { it.drawDepth }
                .forEach { it.handle.medium.render(gc, it.renderRubric) }
    }
}