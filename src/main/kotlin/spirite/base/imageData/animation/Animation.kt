package spirite.base.imageData.animation

import rb.glow.IGraphicsContext
import rb.owl.bindable.Bindable
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.services.AnimationStateBind

abstract class Animation(
        name : String,
        var workspace : IImageWorkspace,
        val stateBind: AnimationStateBind
)
{
    val nameBind = Bindable(name)
    var name by nameBind
    abstract val startFrame : Float
    abstract val endFrame : Float

    abstract fun drawFrame(gc: IGraphicsContext, t: Float)

    protected fun triggerStructureChange() {
        workspace.animationManager.triggerStructureChange(this)
    }

    abstract fun dupe(): Animation
}

abstract class MediumBasedAnimation(name : String, workspace : IImageWorkspace, stateBind: AnimationStateBind = AnimationStateBind())
    : Animation(name, workspace, stateBind)
{

    abstract fun getDrawList( t: Float) : List<TransformedHandle>

    override fun drawFrame(gc: IGraphicsContext, t: Float) {
        getDrawList(t)
                .sortedBy {  it.drawDepth }
                .forEach {it.draw(gc)}
    }
}