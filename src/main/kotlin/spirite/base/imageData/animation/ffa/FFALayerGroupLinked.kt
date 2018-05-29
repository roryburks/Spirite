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
    private var nodeLinks = HashMap<Node, FFAFrame>()
    var includeSubtrees by UndoableChangeDelegate(
            includeSubtrees,
            context.workspace.undoEngine,
            "Change Animation Layer's IncludeSubtrees Structure",
            {groupLinkUpdated()})

    override fun moveFrame(frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean) {
        val tree = context.workspace.groupTree
        val nodeToMove = frameToMove.node ?: return
        when {
            frameRelativeTo == null ->  tree.moveInto(nodeToMove, groupLink, true)
            above -> frameRelativeTo.node?.also { tree.moveAbove( nodeToMove, it)}
            else -> frameRelativeTo.node?.also { tree.moveBelow( nodeToMove, it)}
        }
    }


    internal fun groupLinkUpdated() {
        val oldMap = nodeLinks
        val newMap = HashMap<Node, FFAFrame>()
        _frames.clear()

        fun gluRec( node: GroupNode) : Int {
            var len = 0
            node.children.asReversed().forEach {
                when {
                    it is GroupNode && includeSubtrees -> {
                        val solFrame = oldMap.get(it) ?:
                            FFAFrame(FFAFrameStructure(it, START_LOCAL_LOOP, 0))

                        _frames.add(solFrame)
                        val subLen = gluRec(it)
                        if( solFrame.length < subLen)
                            solFrame.structure = solFrame.structure.copy(length =  subLen)
                        _frames.add( FFAFrame(FFAFrameStructure(null, END_LOCAL_LOOP, 0)))

                        len += solFrame.length
                        newMap[it] = solFrame
                    }
                    it is LayerNode -> {
                        val newFrame = oldMap[it] ?: FFAFrame(FFAFrameStructure(it, FRAME, 1))
                        _frames.add(newFrame)

                        len += newFrame.length
                        newMap[it] = newFrame
                    }
                }
            }
            return len
        }

        gluRec( groupLink)
        context.triggerFFAChange(this)
    }

}