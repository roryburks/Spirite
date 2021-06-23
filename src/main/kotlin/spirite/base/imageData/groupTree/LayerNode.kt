package spirite.base.imageData.groupTree

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.view.IViewSystem

class LayerNode(
    tree: GroupTree,
    trigger: GroupTree.IGroupTreeTrigger,
    viewSystem: IViewSystem,
    undoEngine: IUndoEngine?,
    parent: GroupNode?,
    name: String,
    val layer: Layer
) : Node(tree, trigger, viewSystem, undoEngine, parent, name)
{
    override val imageDependencies: Collection<MediumHandle> get() = layer.imageDependencies

    fun getDrawList() : List<TransformedHandle> {
        val transform = tNodeToContext
        return layer.getDrawList().map { it.stack(transform) }
    }
}