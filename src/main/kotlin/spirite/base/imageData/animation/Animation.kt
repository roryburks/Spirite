package spirite.base.imageData.animation

import rb.glow.IGraphicsContext
import rb.owl.bindable.Bindable
import spirite.base.imageData.TransformedHandle

abstract class Animation( name : String )
{
    val nameBind = Bindable(name)
    var name by nameBind
    abstract val startFrame : Float
    abstract val endFrame : Float

    abstract fun drawFrame(gc: IGraphicsContext, t: Float)

    var onStructureChangeTrigger : ((Animation)->Unit)? = null
//    protected fun triggerStructureChange() {
//        workspace.animationManager.triggerStructureChange(this)
//    }

    abstract fun dupe(): Animation
}

abstract class MediumBasedAnimation(name : String)
    : Animation(name)
{

    abstract fun getDrawList( t: Float) : List<TransformedHandle>

    override fun drawFrame(gc: IGraphicsContext, t: Float) {
        getDrawList(t)
                .sortedBy {  it.drawDepth }
                .forEach {it.draw(gc)}
    }
}