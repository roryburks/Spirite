package spirite.base.graphics.rendering.sources

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.RenderSettings
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.*


/**
 * A RenderSource corresponds to an object which can be rendered and it implements
 * everything needed to perform a Render using certain RenderSettings.
 *
 * Note: It is important that subclasses overload the equals and hashCode methods
 * (or better yet be data classes), so that Identical Calls are correctly identified
 * as being identical.
 */
interface RenderSource {
    val workspace: IImageWorkspace
    val defaultWidth: Int
    val defaultHeight: Int
    val imageDependencies: Collection<MediumHandle>
    val nodeDependencies: Collection<Node>

    fun render( settings: RenderSettings, gc: GraphicsContext)
}

fun getRenderSourceForNode( node: Node, workspace: IImageWorkspace) : RenderSource {
    when( node) {
        is GroupNode -> return GroupNodeSource(node, workspace)
        is LayerNode -> return LayerSource(node.layer, workspace)
        else -> throw Exception("Unrecognizd Node Type")
    }
}