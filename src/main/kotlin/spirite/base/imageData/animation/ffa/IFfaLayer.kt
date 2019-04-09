package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle

interface IFFAFrame {
    val layer: IFfaLayer

    val start: Int
    val length: Int
    val end: Int get() = start + length

    fun getDrawList() : List<TransformedHandle>
}

interface IFfaLayer {
    val anim : FixedFrameAnimation
    val start: Int
    val end: Int

    var asynchronous : Boolean

    val frames: List<IFFAFrame>

    fun getFrameFromLocalMet(met : Int, loop: Boolean = true) : IFFAFrame?
}

interface  IFFAFramev2
{
    val layer: IFfaLayer

    val start: Int
    val length: Int
    val end: Int get() = start + length


}