package spirite.base.graphics.isolation

import rb.global.IContract
import rb.glow.gle.RenderRubric
import rb.owl.bindable.addObserver
import rbJvm.owl.addWeakObserver
import spirite.base.graphics.isolation.IIsolationManager.IsolationState
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart


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

    override val currentIsolator: IIsolator? get() = when( isolationIsActive) {
        true -> workspace.groupTree.selectedNode?.run { SpritePartOnlyIsolator(this)}
        false -> null
    }
//        get() = when(isolationIsActive) {
//            true -> when {
//                isolateCurrentNode -> workspace.groupTree.selectedNode?.let { SingleNodeIsolator(it) }
//                _spriteIsolations.any() -> SpriteIsolator(HashMap(_spriteIsolations), workspace.groupTree.root)
//                else -> null
//            }
//            false -> null
//        }


    override fun clearAllIsolation() {
        _spriteIsolations.clear()
        isolateCurrentNode = false
        triggerIsolationChange()
    }

    private fun triggerIsolationChange() {
        imageObservatory.triggerRefresh(ImageChangeEvent(emptySet(), emptySet(), workspace, true))
    }

    init {
        // No reason this would need to be weak, right?
        workspace.groupTree.selectedNodeBind.addWeakObserver { new, _ ->
            if( isolationIsActive && isolateCurrentNode && new != null)
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

    private val _k = workspace.groupTree.selectedNodeBind.addObserver { newNode, _ ->
        _spriteK?.void()
        _spriteK = null

        _spriteK = ((newNode as? LayerNode)?.layer as? SpriteLayer)?.activePartBind?.addObserver { _, _ ->
            if( isolationIsActive) {
                triggerIsolationChange()
            }
        }

        if( isolationIsActive) {
            triggerIsolationChange()
        }
    }
    private var _spriteK : IContract? = null
}
