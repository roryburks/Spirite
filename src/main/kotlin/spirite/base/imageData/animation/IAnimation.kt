package spirite.base.imageData.animation

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace

interface IAnimation {
    var name : String
    var workspace : IImageWorkspace

    val start : Float
    val end : Float

    fun getDrawList( t: Float) : List<TransformedHandle>
}

fun IAnimation.drawFrame( gc: GraphicsContext, t: Float) {
    gc.pushState()

    getDrawList(t)
            
            .forEach {

    }
}