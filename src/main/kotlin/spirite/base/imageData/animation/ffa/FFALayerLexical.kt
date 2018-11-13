package spirite.base.imageData.animation.ffa

import spirite.base.imageData.groupTree.GroupTree.Node

class FFALayerLexical(
        context: FixedFrameAnimation,
        val group: FixedFrameAnimation,
        lexicon: String)
    : FFALayer(context)
{

    private val lexicalMap : MutableMap<Char, Node> = mutableMapOf()

    private fun buildLexicon(lexicon: String) {
        _frames.clear()
    }

    override fun moveFrame(frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean) {}
    override fun addGapFrameAfter(frameBefore: FFAFrame?, gapLength: Int) {}

}