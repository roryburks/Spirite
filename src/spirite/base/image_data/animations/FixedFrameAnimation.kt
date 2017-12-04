package spirite.base.image_data.animations

import com.sun.org.apache.xpath.internal.operations.Bool
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.renderer.RenderEngine
import spirite.base.image_data.*

class FixedFrameAnimationKt : AnimationKt
{
    private var startFrame : Int = 0
    private var endFrame : Int = 0
    override val StartFrame get() = startFrame.toFloat()
    override val EndFrame get() = endFrame.toFloat()
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
        startFrame = 0
        endFrame = 0
        layers.forEach {
            if( it.start < startFrame) startFrame = it.start
            if( it.end >= endFrame) endFrame = it.end+1
        }
    }

    inner class AnimationLayer(
            includeSubtrees: Boolean
    ) {
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
    }
}