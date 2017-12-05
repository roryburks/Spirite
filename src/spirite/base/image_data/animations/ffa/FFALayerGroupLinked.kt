package spirite.base.image_data.animations.ffa

import spirite.base.image_data.GroupTree
import spirite.base.util.UndoableDelegate

class FFALayerGroupLinked (
        context: FixedFrameAnimation,
        val groupLink : GroupTree.GroupNode,
        private var _includeSubtrees: Boolean,
        frameMap : Map<GroupTree.Node,FFAFrameStructure>? = null)
    : FFALayer(context)
{
    private var nodeLinks = HashMap<GroupTree.Node, FFAFrame>()
    var includeSubtrees  by UndoableDelegate(::_includeSubtrees, ::workspace, "Change Inlcude Subtrees", {groupLinkUdated()})

    init {
        name = groupLink.name
        frameMap?.forEach {nodeLinks[it.key] = FFAFrame(it.value)}
        groupLinkUdated()
    }



    // ===========================
    // ==== Link Interpretation
    //region
    internal fun groupLinkUdated() {
        val oldMap = nodeLinks
        val newMap = HashMap<GroupTree.Node, FFAFrame>()
        _frames.clear()
        _gluRec(oldMap, newMap, groupLink)

        nodeLinks = newMap
        context._triggerChange()
    }
    private fun _gluRec(
            oldMap: HashMap<GroupTree.Node, FFAFrame>,
            newMap: HashMap<GroupTree.Node, FFAFrame>,
            node: GroupTree.GroupNode):Int
    {
        var len=0

        node.children.reversed().forEach  {
            if( it is GroupTree.GroupNode && includeSubtrees) {
                val usingNew = (!oldMap.containsKey(it))
                val solFrame = if( usingNew)
                    FFAFrame(FFAFrameStructure(it, FFAFrameStructure.Marker.START_LOCAL_LOOP, 0))
                else oldMap[it]!!

                _frames.add(solFrame)
                val subLen = _gluRec( oldMap, newMap, it)
                if( usingNew || solFrame.length < subLen)
                    solFrame.length = subLen
                _frames.add( FFAFrame(FFAFrameStructure(null, FFAFrameStructure.Marker.END_LOCAL_LOOP, 0)))

                len += solFrame.length
                newMap.put( it, solFrame)
            }
            else if( it is GroupTree.LayerNode) {
                val newFrame =  if (!oldMap.containsKey(it))
                    FFAFrame( FFAFrameStructure(it, FFAFrameStructure.Marker.FRAME, 1))
                else oldMap[it]!!
                _frames.add(newFrame)

                len += newFrame.length
                newMap.put(it, newFrame)
            }
        }
        return len
    }
    //endregion

}