package spirite.base.imageData.animation.ffa

import rb.extendo.delegates.OnChangeDelegate
import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.*
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.undo.IUndoEngineFeed
import spirite.base.imageData.undo.UndoableChangeDelegate


class FfaLayerGroupLinked(
    context: FixedFrameAnimation,
    val groupLink : GroupNode,
    includeSubtrees: Boolean,
    name: String = groupLink.name,
    private val _undoFeed : IUndoEngineFeed? = null,
    frameMap : Map<Node,FfaFrameStructure>? = null,
    unlinkedClusters: List<UnlinkedFrameCluster>? = null,
    asynchronous: Boolean = false)
    : FFALayer(context, _undoFeed, asynchronous), IFFALayerLinked
{
    // TODO: Make Undoable?
    override var name by OnChangeDelegate(name) { anim.triggerFFAChange(this)}

    override fun shouldUpdate(contract: FFAUpdateContract): Boolean {
        return contract.changedNodes.contains(groupLink) || (includeSubtrees && contract.ancestors.contains(groupLink))
    }

    var includeSubtrees by UndoableChangeDelegate(
            includeSubtrees,
            _undoFeed,
            "Change Animation Layer's IncludeSubtrees Structure")
    { groupLinkUpdated() }

    init {
        groupLinkImported(frameMap, unlinkedClusters)
    }

//    fun moveFrame(frameToMove: FFAFrame, frameRelativeTo: FFAFrame?, above: Boolean) {
//        val tree = anim.workspace.groupTree
//        val nodeToMove = frameToMove.node ?: return
//        when {
//            frameRelativeTo == null ->  tree.moveInto(nodeToMove, groupLink, true)
//            above -> frameRelativeTo.node?.also { tree.moveAbove( nodeToMove, it)}
//            else -> frameRelativeTo.node?.also { tree.moveBelow( nodeToMove, it)}
//        }
//    }

    fun addGapFrameAfter(frameBefore: IFfaFrame?, gapLength: Int) {
        val index = if( frameBefore == null) 0 else (_frames.indexOf(frameBefore) + 1)
        _frames.add(index, FFAFrame(FfaFrameStructure(null, GAP,gapLength)))
        anim.triggerFFAChange(this)
    }

    private fun constructFrameMap() : Map<Node,FfaFrameStructure> =
        _frames.mapNotNull {
            val node = it.node
            when(node) {
                null -> null
                else -> Pair(node, it.structure)
            }
        }.toMap()

    data class UnlinkedFrameCluster(
        val nodeBefore: Node?,
        val unlinkedFrames: List<FfaFrameStructure>)

    private data class UnlinkedFrameClusterDetailed(
        val nodeBefore: Node?,
        val nodeAfter: Node?,
        val parentNode: Node?,
        val unlinkedFrames: List<FfaFrameStructure>)

    private fun getUnlinkedNodeClusters() : List<UnlinkedFrameClusterDetailed> {
        val clusters = mutableListOf<UnlinkedFrameClusterDetailed>()
        var currentNode: Node? = null
        var activeCluster: MutableList<FfaFrameStructure>? = null
        var currentParent: Node? = null

        // Update this as needed
        fun frameIsUnlinked(frame : FFAFrame) = frame.marker == GAP

        for( frame in _frames) {
            if(frameIsUnlinked(frame)) {
                activeCluster = activeCluster ?: mutableListOf()
                activeCluster.add(frame.structure)
            }
            else {
                if( activeCluster != null) {
                    clusters.add( UnlinkedFrameClusterDetailed(currentNode, frame.node, currentParent, activeCluster))
                    activeCluster = null
                }
                currentNode = when(frame.marker) {
                    START_LOCAL_LOOP -> {
                        currentParent = frame.node
                        null
                    }
                    else -> frame.node
                }
            }
        }
        if( activeCluster != null) {
            clusters.add(UnlinkedFrameClusterDetailed(currentNode, null, null, activeCluster))
        }
        return clusters
    }

    private fun groupLinkImported(
        links: Map<Node,FfaFrameStructure>? = null,
        clusters: List<UnlinkedFrameCluster>? = null)
    {
        buildFramesFromGroupTree(links ?: mapOf())

        clusters?.forEach { cluster ->
            val index = if( cluster.nodeBefore == null) 0
                else (_frames.indexOfFirst { it.node == cluster.nodeBefore } + 1)
            _frames.addAll(index, cluster.unlinkedFrames.map { FFAFrame(it) })
        }
    }

    override fun groupLinkUpdated( )
    {
        val oldUnlinkedFrameClusters = getUnlinkedNodeClusters()

        buildFramesFromGroupTree(constructFrameMap())

        // Not very Big-O efficient, but hopefully the data sets we're working on is sufficiently slow
        //  Can create a hash map from node->index if necessary
        for( cluster in oldUnlinkedFrameClusters) {
            val position = when {
                cluster.nodeBefore == null -> when( cluster.parentNode) {
                    null -> 0
                    else -> _frames.indexOfFirst { it.node == cluster.parentNode }
                }
                cluster.nodeAfter != null -> {
                    val beforeIndex = _frames.indexOfFirst { it.node == cluster.nodeBefore }
                    val afterIndex = _frames.indexOfFirst { it.node == cluster.nodeAfter }
                    when {
                        afterIndex == beforeIndex+1 -> afterIndex
                        cluster.parentNode != null -> _frames.indexOfFirst { it.node == cluster.parentNode }
                        else -> beforeIndex + 1
                    }
                }
                else -> _frames.lastIndex
            }
            _frames.addAll(position, cluster.unlinkedFrames.map { FFAFrame(it) })
        }

        anim.triggerFFAChange(this)
    }

    private fun buildFramesFromGroupTree(map: Map<Node,FfaFrameStructure>) {
        _frames.clear()
        fun bffgtRec(node: GroupNode) : Int {
            var len = 0
            node.children.asReversed().forEach {
                when {
                    it is GroupNode && includeSubtrees -> {
                        val solFrame = FFAFrame(map[it] ?: FfaFrameStructure(it, START_LOCAL_LOOP, 0))

                        _frames.add(solFrame)
                        val subLen = bffgtRec(it)
                        if( solFrame.length < subLen)
                            solFrame.structure = solFrame.structure.copy(length =  subLen)
                        _frames.add( FFAFrame(FfaFrameStructure(null, END_LOCAL_LOOP, 0)))

                        len += solFrame.length
                    }
                    it is LayerNode -> {
                        val newFrame =  FFAFrame(map[it] ?:FfaFrameStructure(it, FRAME, 1))
                        _frames.add(newFrame)

                        len += newFrame.length
                    }
                }
            }
            return len
        }
        bffgtRec( groupLink)
    }

    override fun dupe(context: FixedFrameAnimation) = FfaLayerGroupLinked(
            context,
            groupLink,
            includeSubtrees,
            name,
            _undoFeed,
            _frames.mapNotNull { when( val node = it.node) {
                null -> null
                else -> Pair(node, it.structure)
            }}.toMap(),
            null,
            asynchronous)
}