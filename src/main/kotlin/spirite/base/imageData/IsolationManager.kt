package spirite.base.imageData

import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IIsolationManager.IsolationState
import spirite.base.imageData.IsolationManager.SpriteIsolationStruct
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.groupExtensions.then
import spirite.base.util.groupExtensions.toHashMap

/**
 *
 */
interface IIsolationManager
{
    data class IsolationState( val isDrawn: Boolean, val alpha: Float)

    var isolationIsActive: Boolean
    val currentIsolator: IIsolator?

    var isolateCurrentNode : Boolean
    fun clearAllIsolation()

    fun getIsolationStateForSpritePartKind(root: GroupNode, partName: String) : IsolationState
    fun setIsolationStateForSpritePartKind(root: GroupNode, partName: String, includeSubtree: Boolean, state: IsolationState)
    fun setIsolationStateForAllSpriteKindsBut(root: GroupNode, partName: String, includeSubtree: Boolean, state: IsolationState)
}

interface IIsolator
{
    fun getIsolatorForNode(node: Node) : IIsolator
    val isDrawn: Boolean
    val rubric: RenderRubric?
}

interface ISpriteLayerIsolator: IIsolator {
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
            true -> when {
                isolateCurrentNode -> workspace.groupTree.selectedNode?.let { SingleNodeIsolator(it)}
                _spriteIsolations.any() -> SpriteIsolator(HashMap(_spriteIsolations), workspace.groupTree.root)
                else -> null
            }
            false -> null
        }


    override fun clearAllIsolation() {
        _spriteIsolations.clear()
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

    internal data class SpriteIsolationStruct(
            val root: GroupNode,
            val partName: String,
            val isDrawn: Boolean,
            val alpha: Float,
            val includeSubtree: Boolean,
            val inverted: Boolean)
    private  val _spriteIsolations  = mutableMapOf<Pair<GroupNode,String?>,SpriteIsolationStruct>()

    override fun getIsolationStateForSpritePartKind(root: GroupNode, partName: String): IsolationState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setIsolationStateForSpritePartKind(root: GroupNode, partName: String, includeSubtree: Boolean, state: IsolationState) {
        _spriteIsolations[Pair(root,partName)] = SpriteIsolationStruct(root, partName, state.isDrawn, state.alpha, includeSubtree, false)
        isolationIsActive = true
        triggerIsolationChange()
    }

    override fun setIsolationStateForAllSpriteKindsBut(root: GroupNode, partName: String, includeSubtree: Boolean, state: IsolationState) {
        _spriteIsolations[Pair(root,null)] = SpriteIsolationStruct(root, partName, state.isDrawn, state.alpha, includeSubtree, true)
        isolationIsActive = true
        triggerIsolationChange()
    }

}

internal class SpriteIsolator(
        private val map: HashMap<Pair<GroupNode,String?>, SpriteIsolationStruct>,
        val node: Node) : ISpriteLayerIsolator
{
    override fun getIsolatorForNode(node: Node): IIsolator {
        return SpriteIsolator(map, node)
    }
    override fun getIsolationForPart(part: SpritePart): IIsolator {
        val ancestors = if(node is GroupNode) node.ancestors.then(node) else node.ancestors

        var isDrawn = true
        var alpha = 1f
        for (ancestor in ancestors) {
            val isolation = map[Pair(ancestor,part.partName)]

            if( isolation != null && (isolation.includeSubtree || ancestor == node.parent)) {
                if( !isolation.isDrawn) {
                    isDrawn = false
                    break
                }
                alpha *= isolation.alpha
            }

            val invertIsolation = map[Pair(ancestor,null)]
            if( invertIsolation != null &&
                    (invertIsolation.includeSubtree || ancestor == node.parent) &&
                    (invertIsolation.partName != part.partName))
            {
                if( !invertIsolation.isDrawn) {
                    isDrawn = false
                    break
                }
                alpha *= invertIsolation.alpha
            }
        }

        return when {
            !isDrawn -> NilNodeIsolator
            alpha == 1f -> TrivialNodeIsolator
            else -> ExplicitIsolator(RenderRubric(alpha = alpha))
        }
    }

    override val isDrawn: Boolean get() = true
    override val rubric: RenderRubric? get() = null
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
class ExplicitIsolator(override val rubric: RenderRubric) : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = true
}
object NilNodeIsolator : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = false
    override val rubric: RenderRubric? = null
}