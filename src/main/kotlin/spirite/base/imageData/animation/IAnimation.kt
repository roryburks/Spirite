package spirite.base.imageData.animation

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace

abstract class Animation(
        var name : String,
        var workspace : IImageWorkspace )
{
    abstract val startFrame : Float
    abstract val endFrame : Float

    abstract fun drawFrame( gc: GraphicsContext, t: Float)
}

abstract class MediumBasedAnimation(name : String,workspace : IImageWorkspace)
    : Animation(name, workspace)
{

    abstract fun getDrawList( t: Float) : List<TransformedHandle>

    override fun drawFrame( gc: GraphicsContext, t: Float) {
        getDrawList(t)
                .sortedBy { -it.drawDepth }
                .forEach {it.draw(gc)}
    }
}