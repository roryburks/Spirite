package spirite.base.image_data.animations.ffa

import spirite.base.graphics.renderer.RenderEngine
import spirite.base.image_data.*
import spirite.base.image_data.animations.NodeLinkedAnimation

class FixedFrameAnimation(name: String, workspace: ImageWorkspace) : Animation(name, workspace),
        NodeLinkedAnimation
{

    var start : Int = 0
        private set
    var end : Int = 0
        private set
    override val StartFrame get() = start.toFloat()
    override val EndFrame get() = end.toFloat()
    override val isFixedFrame = true

    private val _layers = ArrayList<FFALayer>()
    public val layers get() = _layers.toList()

    fun addLinkedLayer( link: GroupTree.GroupNode, includeSubtrees: Boolean, frameMap : Map<GroupTree.Node,FFAFrameStructure>? = null)
    {
        _layers.add( FFALayerGroupLinked(this, link, includeSubtrees, frameMap))
        _triggerChange()
    }


    override fun getDrawList(t: Float): List<RenderEngine.TransformedHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    internal fun _triggerChange() {
        recalculateMetrics()
        triggerChange()
    }
    private fun recalculateMetrics() {
        start = 0
        end = 0
        _layers.forEach {
            if( it.start < start) start = it.start
            if( it.end >= end) end = it.end+1
        }
    }

    // :::: NodeLinkedAnimation
    override fun nodesChanged(changed: List<GroupTree.Node>) {
        val groupedLayers = _layers.filter { it is FFALayerGroupLinked }.map{it as FFALayerGroupLinked}
        changed.forEach { changedNode -> groupedLayers.filter { it.groupLink == changedNode }.forEach {it.groupLinkUdated()} }
    }
}