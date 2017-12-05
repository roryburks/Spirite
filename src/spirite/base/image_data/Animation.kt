package spirite.base.image_data

import com.sun.corba.se.impl.orbutil.graph.Graph
import com.sun.org.apache.xpath.internal.operations.Bool
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.renderer.RenderEngine

abstract class Animation constructor(
        var name: String,
        var context: ImageWorkspace)
{
    abstract val StartFrame : Float
    abstract val EndFrame : Float
    abstract val isFixedFrame: Boolean

    val animationManager :AnimationManager get() = context.animationManager

    protected fun triggerChange() { animationManager?.triggerChangeAnimation(this)}

    fun drawFrame( gc: GraphicsContext, t: Float) {
        gc.pushState()

        this.getDrawList(t).forEach {
            gc.setComposite( gc.composite, it.alpha)
            it.handle.drawLayer(gc, it.trans)
        }

        gc.popState()
    }
    abstract fun getDrawList( t: Float) : List<RenderEngine.TransformedHandle>
}