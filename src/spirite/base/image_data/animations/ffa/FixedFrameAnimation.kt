package spirite.base.image_data.animations.ffa

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle
import spirite.base.image_data.Animation
import spirite.base.image_data.GroupTree
import spirite.base.image_data.ImageWorkspace
import spirite.base.image_data.animations.NodeLinkedAnimation
import spirite.base.util.MUtil
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import kotlin.math.floor

class FixedFrameAnimation(name: String, workspace: ImageWorkspace) : Animation(name, workspace),
        NodeLinkedAnimation
{
    val start get() = _start
    val end get() = _end-1
    override val StartFrame get() = start.toFloat()
    override val EndFrame get() = _end-1f
    override val isFixedFrame = true

    private val _layers = ArrayList<FFALayer>()
    public val layers get() = _layers.toList()

    fun addLinkedLayer( link: GroupTree.GroupNode, includeSubtrees: Boolean, frameMap : Map<GroupTree.Node,FFAFrameStructure>? = null)
    {
        _layers.add( FFALayerGroupLinked(this, link, includeSubtrees, frameMap))
        _triggerChange()
    }


    override fun getDrawList(t: Float): List<TransformedHandle> {
        val _t = floor(t).toInt()
        val met = MUtil.cycle( start, end, _t)
        val drawList = ArrayList<TransformedHandle>()

        for( layer in _layers){
            if(layer.frames.isEmpty())
                continue

            val start = layer.start
            val end = layer.end

            // Based on the layer timing type, determine the local frame
            //	index to use (if any)
            val localMet = if( layer.asynchronous) MUtil.cycle(start, end, _t) else met

            val node = layer.getFrameForMet(localMet)?.node

            if( node is GroupTree.LayerNode) {
                for( tr in node.layer.drawList) {
                    tr.trans.preTranslate(node.offsetX.toFloat(), node.offsetY.toFloat())
                    drawList.add( tr)
                }
            }
        }

        drawList.sortBy { it.depth }
        return drawList
    }

    internal fun _triggerChange() {
        metricsCalculated = false
        triggerChange()
    }

    private var metricsCalculated = false
    private var _start : Int = 0
        get() {
            if(!metricsCalculated) recalculateMetrics()
            return field
        }
    private var _end : Int = 0
        get() {
            if(!metricsCalculated) recalculateMetrics()
            return field
        }
    private fun recalculateMetrics() {
        metricsCalculated = true
        _start = 0
        _end = 0
        _layers.forEach {
            if( it.start < _start) _start = it.start
            if( it.end >= _end) _end = it.end+1
        }
    }

    // :::: NodeLinkedAnimation
    override fun nodesChanged(changed: List<GroupTree.Node>) {
        // Just updates all linked Layers.  Could be more discriminating
        val groupedLayers = _layers.filter { it is FFALayerGroupLinked }.map{it as FFALayerGroupLinked}
        groupedLayers.forEach { it.groupLinkUdated() }
    }
}