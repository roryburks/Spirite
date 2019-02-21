package spirite.base.imageData.animation.ffa

interface IFFAFrame {
    val context: IFFALayer

    val start: Int
    val end: Int

    val next: IFFAFrame?
    val previous : IFFAFrame?

    val structure : FFAFrameStructure

    var length: Int
}

interface IFFALayer {
    val frames: List<IFFAFrame>
}