package spirite.base.graphics.rendering.sources

import rb.extendo.dataStructures.SinglyList
import rb.glow.IGraphicsContext
import spirite.base.graphics.rendering.NodeRenderer
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.Node

data class GroupNodeSource(val group: GroupNode, override val workspace: IImageWorkspace) : IRenderSource {
    override val defaultWidth: Int get() = workspace.width
    override val defaultHeight: Int get() = workspace.width
    override val imageDependencies: Collection<MediumHandle> get() = group.imageDependencies
    override val nodeDependencies: Collection<Node> get() = SinglyList(group) + group.getAllAncestors()
    override val rendersLifted: Boolean get() = true

    override fun render(settings: RenderSettings, gc: IGraphicsContext) {
        NodeRenderer( group, workspace, settings, workspace.isolationManager.currentIsolator).render(gc)
    }
}

