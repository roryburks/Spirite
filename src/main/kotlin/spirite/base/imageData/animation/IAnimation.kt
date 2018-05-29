package spirite.base.imageData.animation

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace

interface IAnimation {
    var name : String
    var workspace : IImageWorkspace

    val startFrame : Float
    val endFrame : Float

    fun getDrawList( t: Float) : List<TransformedHandle>
}

fun IAnimation.drawFrame( gc: GraphicsContext, t: Float) {
    getDrawList(t)
            .sortedBy { -it.drawDepth }
            .forEach {it.draw(gc)}
}