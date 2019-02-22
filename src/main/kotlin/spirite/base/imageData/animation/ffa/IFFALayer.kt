package spirite.base.imageData.animation.ffa

interface IFFAFrame {
    val layer: IFFALayer

    val start: Int
    val end: Int

    val next: IFFAFrame?
    val previous : IFFAFrame?

    val structure : FFAFrameStructure

    var length: Int
}

interface IFFALayer {
    val start: Int
    val end: Int

    var asynchronous : Boolean

    val frames: List<IFFAFrame>

    fun getFrameFromLocalMet(met : Int, loop: Boolean = true) : IFFAFrame?
}