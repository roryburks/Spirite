package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.MediumBasedAnimation
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.util.f
import spirite.base.util.groupExtensions.mapAggregated

class FixedFrameAnimation(name: String, workspace: IImageWorkspace)
    : MediumBasedAnimation(name, workspace)
{
    var start : Int = 0 ; private set
    var end : Int = 0 ; private set

    constructor(name: String, workspace: IImageWorkspace, node : GroupNode) : this(name, workspace){

    }
    constructor(name: String, workspace: IImageWorkspace, layers: List<FFALayer>) : this(name, workspace){

    }

    override val startFrame: Float get() = start.f
    override val endFrame: Float get() = end.f

    val _layers = mutableListOf<FFALayer>()

    override fun getDrawList(t: Float): List<TransformedHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

