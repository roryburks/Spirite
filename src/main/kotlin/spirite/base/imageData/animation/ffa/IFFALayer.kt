package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle
import sun.security.util.Length

interface IFFAFrame {
    val layer: IFFALayer

    val start: Int
    val length: Int
    val end: Int get() = start + length

    val next: IFFAFrame?
    val previous : IFFAFrame?

    fun getDrawList() : List<TransformedHandle>
}

interface IFFALayer {
    val anim : FixedFrameAnimation
    val start: Int
    val end: Int

    var asynchronous : Boolean

    val frames: List<IFFAFrame>

    fun getFrameFromLocalMet(met : Int, loop: Boolean = true) : IFFAFrame?
}

interface  IFFAFramev2
{
    val layer: IFFALayer

    val start: Int
    val length: Int
    val end: Int get() = start + length


}