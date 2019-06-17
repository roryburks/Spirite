package spirite.base.imageData.animation

import rb.owl.bindable.Bindable
import rb.glow.GraphicsContext
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace

abstract class Animation(
        name : String,
        var workspace : IImageWorkspace,
        val state: AnimationState)
{
    val nameBind = Bindable(name)
    var name by nameBind
    abstract val startFrame : Float
    abstract val endFrame : Float

    abstract fun drawFrame(gc: GraphicsContext, t: Float)

    protected fun triggerStructureChange() {
        workspace.animationManager.triggerStructureChange(this)
    }

    abstract fun dupe(): Animation
}

abstract class MediumBasedAnimation(name : String,workspace : IImageWorkspace, state: AnimationState = AnimationState())
    : Animation(name, workspace, state)
{

    abstract fun getDrawList( t: Float) : List<TransformedHandle>

    override fun drawFrame(gc: GraphicsContext, t: Float) {
        getDrawList(t)
                .sortedBy {  it.drawDepth }
                .forEach {it.draw(gc)}
    }
}