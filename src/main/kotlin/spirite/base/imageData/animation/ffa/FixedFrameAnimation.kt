package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.MediumBasedAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.groupExtensions.mapAggregated
import kotlin.math.floor

class FixedFrameAnimation(name: String, workspace: IImageWorkspace)
    : MediumBasedAnimation(name, workspace)
{
    constructor(name: String, workspace: IImageWorkspace, node : GroupNode) : this(name, workspace){
        addLinkedLayer(node, false)
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

    fun treeChanged( changedNodes : Set<Node>) {
        val ancestors by lazy {changedNodes.mapAggregated { it.ancestors}.union(changedNodes)}

        _layers.filterIsInstance<FFALayerGroupLinked>()
                .filter { changedNodes.contains(it.groupLink) || (it.includeSubtrees && ancestors.contains(it.groupLink)) }
                .forEach { it.groupLinkUpdated() }
    }

    internal fun triggerFFAChange( layer: FFALayer?) {
        triggerStructureChange()
    }

    fun addLinkedLayer( group: GroupNode, includeSubtrees: Boolean, frameMap: Map<Node, FFAFrameStructure>? = null)
    {
        val layer = FFALayerGroupLinked(this, group, includeSubtrees, frameMap)
        _layers.add(layer)
        triggerFFAChange(layer)
    }
}

