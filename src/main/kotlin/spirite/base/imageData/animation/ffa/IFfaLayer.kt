package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle

interface IFfaFrame {
    val layer: IFfaLayer

    val start: Int
    val length: Int
    val end: Int get() = start + length

    fun getDrawList() : List<TransformedHandle>

}

interface IFfaLayer {
    var name : String
    val anim : FixedFrameAnimation
    val start: Int
    val end: Int

    var asynchronous : Boolean

    val frames: List<IFfaFrame>

    fun getFrameFromLocalMet(met : Int, loop: Boolean = true) : IFfaFrame?

    fun dupe(context: FixedFrameAnimation) : IFfaLayer
}