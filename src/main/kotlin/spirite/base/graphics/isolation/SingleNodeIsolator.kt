package spirite.base.graphics.isolation

import rb.jvm.owl.addWeakObserver
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart

class NodeIsolator(
        val selectedNode: Node)  : IIsolator, ISpriteLayerIsolator
{
    val ancestors by lazy { selectedNode.ancestors.toHashSet() }
    val selectedPartName by lazy { ((selectedNode as? LayerNode)?.layer as? SpriteLayer)?.activePart?.partName }

    override fun getIsolationForPart(part: SpritePart) = when {
        part.partName == selectedPartName -> TrivialNodeIsolator
        else -> NilNodeIsolator
    }

    override fun getIsolatorForNode(node: Node) = when {
        selectedNode == node -> this
        ancestors.contains(node) -> this
        else -> NilNodeIsolator
    }

    override val isDrawn: Boolean get() = true
    override val rubric: RenderRubric? get() = null
}


class SpritePartOnlyIsolator(
        val selectedNode: Node)  : IIsolator, ISpriteLayerIsolator
{
    val ancestors by lazy { selectedNode.ancestors.toHashSet() }
    val selectedPartName by lazy { ((selectedNode as? LayerNode)?.layer as? SpriteLayer)?.activePart?.partName }

    override fun getIsolationForPart(part: SpritePart) = when {
        part.partName == selectedPartName -> TrivialNodeIsolator
        else -> NilNodeIsolator
    }

    override fun getIsolatorForNode(node: Node) = this

    override val isDrawn: Boolean get() = true
    override val rubric: RenderRubric? get() = null
}