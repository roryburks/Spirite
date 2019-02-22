package spirite.base.imageData.animation.ffa

import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupTree.GroupNode

class FFALayerCascading(
        context: FixedFrameAnimation,
        val groupLink: GroupNode,
        lexicon: String = "")
    :IFFALayer
{
    override val start: Int
        get() = TODO("not implemented")
    override val end: Int
        get() = TODO("not implemented")
    override var asynchronous: Boolean
        get() = TODO("not implemented")
        set(value) {}
    override val frames: List<IFFAFrame>
        get() = TODO("not implemented")

    override fun getFrameFromLocalMet(met: Int, loop: Boolean): IFFAFrame? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}