package spirite.base.imageData.animation.ffa

import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.delegates.UndoableChangeDelegate


class FFALayerGroupLinked(
        context: FixedFrameAnimation,
        val groupLink : GroupNode,
        includeSubtrees: Boolean,
        frameMap : Map<Node,FFAFrameStructure>? = null)
    : FFALayer(context)
{
    var includeSubtrees by UndoableChangeDelegate(
            includeSubtrees,
            context.workspace.undoEngine,
            "Change Animation Layer's IncludeSubtrees Structure")
            {groupLinkUpdated()}

    init {
        groupLinkUpdated(frameMap)
    }

    override fun moveFrame(frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean) {
        val tree = context.workspace.groupTree
        val nodeToMove = frameToMove.node ?: return
        when {
            frameRelativeTo == null ->  tree.moveInto(nodeToMove, groupLink, true)
            above -> frameRelativeTo.node?.also { tree.moveAbove( nodeToMove, it)}
            else -> frameRelativeTo.node?.also { tree.moveBelow( nodeToMove, it)}
        }
    }

    private fun constructFrameMap() : Map<Node,FFAFrameStructure> =
        frames.mapNotNull {
            val node = it.node
            when(node) {
                null -> null
                else -> Pair(node, it.structure)
            }
        }.toMap()

    internal fun groupLinkUpdated( links: Map<Node,FFAFrameStructure>? = null) {
        val oldMap = links ?: constructFrameMap()
        val newMap = HashMap<Node, FFAFrame>()
        _frames.clear()

        fun gluRec( node: GroupNode) : Int {
            var len = 0
            node.children.asReversed().forEach {
                when {
                    it is GroupNode && includeSubtrees -> {
                        val solFrame = FFAFrame(oldMap[it] ?: FFAFrameStructure(it, START_LOCAL_LOOP, 0))

                        _frames.add(solFrame)
                        val subLen = gluRec(it)
                        if( solFrame.length < subLen)
                            solFrame.structure = solFrame.structure.copy(length =  subLen)
                        _frames.add( FFAFrame(FFAFrameStructure(null, END_LOCAL_LOOP, 0)))

                        len += solFrame.length
                    }
                    it is LayerNode -> {
                        val newFrame =  FFAFrame(oldMap[it] ?:FFAFrameStructure(it, FRAME, 1))
                        _frames.add(newFrame)

                        len += newFrame.length
                    }
                }
            }
            return len
        }

        gluRec( groupLink)
        context.triggerFFAChange(this)
    }

}