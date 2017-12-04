package spirite.base.image_data

import com.sun.corba.se.impl.orbutil.graph.Graph
import com.sun.org.apache.xpath.internal.operations.Bool
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.renderer.RenderEngine

abstract class AnimationKt constructor( context: ImageWorkspace)
{
    var context = context
        set( value)  {
            context = value
            this.animationManager = value?.animationManager
        }
    var animationManager : AnimationManager? = context.animationManager
        private set


    abstract val StartFrame : Float
    abstract val EndFrame : Float
    abstract val IsFixedFrame : Boolean
    lateinit var name : String

//    protected fun triggerChange() { animationManager?.triggerChangeAnimation(this)}

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