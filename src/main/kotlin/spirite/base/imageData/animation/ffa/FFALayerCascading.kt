package spirite.base.imageData.animation.ffa

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.LayerNode

class FFALayerCascading(
        override val anim: FixedFrameAnimation,
        val groupLink: GroupNode,
        lexicon: String = "")
    :IFFALayer, IFFALayerLinked
{
    // region IFFALayer
    override val start: Int get() = 0
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
    // endregion

    // region IFFALayerLinked
    override fun groupLinkUpdated() {
        groups = groupLink.children.filterIsInstance<GroupNode>()
    }

    override fun shouldUpdate(contract: FFAUpdateContract) = contract.ancestors.contains(groupLink)
    // Endregion


    var lexicon: String = ""
        set(value) {
            field = value
            anim.triggerFFAChange(this)
        }

    var groups = listOf<GroupNode>()

    inner class CascadingFrame(
            override val start: Int,
            val layers : List<LayerNode>
    )
        :IFFAFrame
    {
        override val layer: IFFALayer get() = this@FFALayerCascading
        override val length: Int get() = 1
        override fun getDrawList() = layers.flatMap { it.getDrawList() }
    }

}