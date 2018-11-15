package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.MediumBasedAnimation
import spirite.base.imageData.animation.ffa.FFALayerGroupLinked.UnlinkedFrameCluster
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.groupExtensions.mapAggregated

class FixedFrameAnimation(name: String, workspace: IImageWorkspace)
    : MediumBasedAnimation(name, workspace)
{
    constructor(name: String, workspace: IImageWorkspace, node : GroupNode) : this(name, workspace){
        addLinkedLayer(node, true)
    }
    constructor(name: String, workspace: IImageWorkspace, layers: List<FFALayer>) : this(name, workspace){
        _layers.addAll(layers)
    }

    private val _layers = mutableListOf<FFALayer>()
    val layers : List<FFALayer> get() = _layers

    val start : Int get() = _layers.map { it.start }.min() ?: 0
    val end : Int get() = _layers.map { it.end }.max() ?: 0
    override val startFrame: Float get() = start.f
    override val endFrame: Float get() = end.f

    override fun getDrawList(t: Float): List<TransformedHandle> {
        val _t = t.floor
        val met = MathUtil.cycle(start, end, _t)
        val drawList = mutableListOf<TransformedHandle>()

        _layers
                .filter { it.frames.any() }
                .map { layer ->
                    val localMet = if( layer.asynchronous) MathUtil.cycle(layer.start, layer.end, _t) else met
                    layer.getFramFromLocalMet(localMet)?.node }
                .filterIsInstance<LayerNode>()
                .forEach { drawList.addAll(it.getDrawList()) }

        return drawList
    }

    class FFAUpdateContract(val changedNodes: Set<Node>)
    {
        val ancestors by lazy {changedNodes.mapAggregated { it.ancestors}.union(changedNodes)}
    }
    fun treeChanged( changedNodes : Set<Node>) {
        val contract = FFAUpdateContract(changedNodes)

        _layers.asSequence()
                .filterIsInstance<IFFALayerLinked>()
                .filter { it.shouldUpdate(contract) }
                .forEach { it.groupLinkUpdated() }
    }

    internal fun triggerFFAChange( layer: FFALayer?) {
        triggerStructureChange()
    }

    fun addLinkedLayer(
            group: GroupNode,
            includeSubtrees: Boolean,
            frameMap: Map<Node, FFAFrameStructure>? = null,
            unlinkedClusters: List<UnlinkedFrameCluster>? = null)
    {
        val layer = FFALayerGroupLinked(this, group, includeSubtrees, frameMap, unlinkedClusters)
        _layers.add(layer)
        triggerFFAChange(layer)
    }

    override fun dupe(): Animation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

