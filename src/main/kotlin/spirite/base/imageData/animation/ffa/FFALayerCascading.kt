package spirite.base.imageData.animation.ffa

import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupTree.GroupNode

class FFALayerCascading(
        context: FixedFrameAnimation,
        val groupLink: GroupNode,
        lexicon: String = "")
    :FFALayer(context), IFFALayerLinked
{
    override fun groupLinkUpdated() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shouldUpdate(contract: FFAUpdateContract) = contract.changedNodes.contains(groupLink)

    override fun moveFrame(frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addGapFrameAfter(frameBefore: FFAFrame?, gapLength: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}