package spirite.base.image_data.animations

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

    private val layers = ArrayList<AnimationLayer>()

    constructor(group: GroupTree.GroupNode, name: String, includeSubtrees: Boolean) : super(group.context) {
        this.name = name
    }

    override fun getDrawList(t: Float): List<RenderEngine.TransformedHandle> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun constructFromGroup( group: GroupTree.GroupNode, includeSubtrees: Boolean) {
        val layer = AnimationLayer(includeSubtrees)
        layer.groupLink = group
    }

    private fun _triggerChange() {
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

    inner class AnimationLayer(
            includeSubtrees: Boolean
    )
    {
        var start = 0
            private set
        var end = 0
            private set

        var name :String? = null
            get() = field ?: groupLink?.name ?: "Unnamed Layer"
        var asynchronous = false


        var includeSubtrees = includeSubtrees
        set(value)  {
            val oldInclude = field
            context.undoEngine.performAndStore( object: UndoEngine.NullAction() {
                override fun performAction() {
                    field = value
                    _triggerChange()
                }
                override fun undoAction() {
                    field = oldInclude
                    _triggerChange()
                }
            })
        }


        var groupLink : GroupTree.GroupNode? = null
        set(value)  {

        }
    }
}