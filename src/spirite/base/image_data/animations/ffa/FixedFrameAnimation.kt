package spirite.base.image_data.animations.ffa

import spirite.base.graphics.renderer.RenderEngine
import spirite.base.image_data.*

class FixedFrameAnimation : Animation
{
    var start : Int = 0
        private set
    var end : Int = 0
        private set
    override val StartFrame get() = start.toFloat()
    override val EndFrame get() = end.toFloat()
    override val IsFixedFrame = true

    private val layers = ArrayList<FFALayer>()

    constructor(group: GroupTree.GroupNode, name: String, includeSubtrees: Boolean) : super(group.context) {
        this.name = name
    }

    override fun getDrawList(t: Float): List<RenderEngine.TransformedHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun constructFromGroup( group: GroupTree.GroupNode, includeSubtrees: Boolean) {
        val layer = FFALayer(includeSubtrees, this)
        layer.groupLink = group
    }

    internal fun _triggerChange() {
        recalculateMetrics()
        //triggerChange()

    }
    private fun recalculateMetrics() {
        start = 0
        end = 0
        layers.forEach {
            if( it.start < start) start = it.start
            if( it.end >= end) end = it.end+1
        }
    }

}