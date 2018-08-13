package spirite.base.imageData

import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart

/**
 *
 */
interface IIsolationManager
{
    var isolationIsActive: Boolean
    val currentIsolator: IIsolator?

    var isolateCurrentNode : Boolean
    fun clearAllIsolation()

//    fun resetIsolationForSprites(root: GroupNode)
//    fun setIsolationStateForSpritePartKind(root: GroupNode, partName: String, includeSubtrees: Boolean, isDrawn: Boolean, renderRubric: RenderRubric)
//    fun setDefaultIsolationStateForSprite(root: GroupNode, includeSubtrees: Boolean, isDrawn: Boolean, renderRubric: RenderRubric)
}

interface IIsolator
{
    fun getIsolatorForNode(node: Node) : IIsolator
    val isDrawn: Boolean
    val rubric: RenderRubric?
}

interface ISpriteLayerIsolator {
    fun getIsolationForPart(part: SpritePart) : IIsolator
}

class IsolationManager(
        private val workspace: IImageWorkspace)
    : IIsolationManager
{

    private val imageObservatory get() = workspace.imageObservatory

    override var isolationIsActive: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                triggerIsolationChange()
            }
        }
    override var isolateCurrentNode: Boolean = false
        set(value) {
            if( field != value) {
                field = value
                isolationIsActive = true
                triggerIsolationChange()
            }
        }

    override val currentIsolator: IIsolator?
        get() = when(isolationIsActive) {
            true -> when( isolateCurrentNode) {
                false -> null
                else ->workspace.groupTree.selectedNode?.let { SingleNodeIsolator(it)}
            }
            false -> null
        }


    override fun clearAllIsolation() {
        isolateCurrentNode = false
        triggerIsolationChange()
    }

    private fun triggerIsolationChange() {
        imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), emptySet(), workspace, true))
    }

    private val __ref1  = workspace.groupTree.selectedNodeBind.addWeakListener { new, _ ->
        if( isolationIsActive && isolateCurrentNode && new != null) {
            triggerIsolationChange()
        }
    }
}

class SingleNodeIsolator( private val nodeToIsolate: Node) : IIsolator {
    override fun getIsolatorForNode(node: Node) = when(node) {
        nodeToIsolate -> TrivialNodeIsolator
        else -> this
    }

    override val isDrawn: Boolean = false
    override val rubric: RenderRubric? get() = null
}
object TrivialNodeIsolator : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = true
    override val rubric: RenderRubric? = null
}
object NilNodeIsolator : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = false
    override val rubric: RenderRubric? = null
}