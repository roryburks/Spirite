package spirite.base.imageData.animation.ffa

import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.FRAME
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.GAP
import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupTree.*

class FFALayerLexical(
        context: FixedFrameAnimation,
        val group: FixedFrameAnimation,
        val groupLink: GroupNode,
        lexicon: String)
    : FFALayer(context), IFFALayerLinked
{
    var lexicon: String = lexicon
        set(value) {
            if( field != value) {
                field = value
                buildLexicon(value)
            }
        }

    // region IFFALayerLinked
    override fun shouldUpdate(contract: FFAUpdateContract): Boolean = contract.changedNodes.contains(groupLink)
    override fun groupLinkUpdated() {
        lexicalMap.clear()
        var i = 0
        for ( node in groupLink.children.asSequence().filterIsInstance<LayerNode>()) {
            if( i++ >= 26)
                break
            lexicalMap['A'+i] = node
        }

        buildLexicon(lexicon)
    }
    // endregion

    private val lexicalMap : MutableMap<Char, Node> = mutableMapOf()

    private fun buildLexicon(lexicon: String) {
        _frames.clear()

        lexicon.asSequence()
                .mapNotNull {
                    when( it) {
                        ' ', '_', '-' -> FFAFrameStructure(null, GAP, 1)
                        else -> {
                            val node = lexicalMap[it] ?: return@mapNotNull null
                            FFAFrameStructure(node, FRAME, 1)
                        }
                    }
                }
                .forEach { _frames.add(FFAFrame(it)) }

        context.triggerFFAChange(this)
    }


    override fun moveFrame(frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean) {}
    override fun addGapFrameAfter(frameBefore: FFAFrame?, gapLength: Int) {}

}