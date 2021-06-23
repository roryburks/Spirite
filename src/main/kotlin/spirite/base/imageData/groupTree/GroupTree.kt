package spirite.base.imageData.groupTree

import rb.extendo.dataStructures.Deque
import rb.owl.IObservable
import rb.owl.Observable
import spirite.sguiHybrid.MDebug
import spirite.sguiHybrid.MDebug.ErrorType.STRUCTURAL
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.view.IViewSystem

/**
 * The GroupTree is an abstract Container Structure used by several components to organize ImageData in a hierarchical
 * tree structure.
 */
open class GroupTree(
        val undoEngine: IUndoEngine?,

        // Note: Could potentially de-couple the Group tree from the view system, but that would require re-doing
        // everything that accesses view properties direct from the node
        val viewSystem: IViewSystem)
{
    val root : GroupNode
    open val treeDescription = "Abstract Group Tree"

    // TODO: right now this is coupled to the view system in an unpleasant way
    val selectedNodeBind get() = viewSystem.currentNodeBind
    var selectedNode
        get() = selectedNodeBind.field
        set(value) {
            when {
                value == null -> selectedNodeBind.field = null
                value.tree != this -> MDebug.handleError(STRUCTURAL, "Tried to splice a node into a new tree")
                else -> selectedNodeBind.field = value
            }
        }

    // region Tree Observer
    interface TreeObserver {
        fun treeStructureChanged(evt: TreeChangeEvent)
        fun nodePropertiesChanged(node: Node, renderChanged: Boolean)
    }
    class TreeChangeEvent( val changedNodes : Set<Node>)
    val treeObservable : IObservable<TreeObserver> get() = _treeObs
    private val _treeObs = Observable<TreeObserver>()

    interface IGroupTreeTrigger
    {
        fun triggerChange(evt : TreeChangeEvent)
        fun triggerNodeAttributeChanged(node: Node, renderChanged: Boolean)
    }
    private val _trigger = object : IGroupTreeTrigger {
        override fun triggerChange(evt: TreeChangeEvent) {_treeObs.trigger { it.treeStructureChanged(evt) } }
        override fun triggerNodeAttributeChanged(node: Node, renderChanged: Boolean)
            {_treeObs.trigger { it.nodePropertiesChanged(node, renderChanged) }}
    }
    // endregion

    init {
        root = GroupNode( this, _trigger, viewSystem, undoEngine, null, "ROOT")
    }

    fun makeLayerNode(parent: GroupNode?, name:String, layer: Layer) =
        LayerNode(this, _trigger, viewSystem, undoEngine, parent, name, layer)
    fun makeGroupNode(parent: GroupNode?, name: String) =
        GroupNode(this, _trigger, viewSystem, undoEngine, parent, name)
}

fun GroupNode.traverse() : Sequence<Node> = GroupNodeTraversalSequence(this)
fun GroupNode.traverse(filter: (Node) -> Boolean) : Sequence<Node> = GroupNodeTraversalSequence(this, filter)

private class GroupNodeTraversalSequence(
    val groupNode: GroupNode,
    val filter: ((Node)->Boolean)? = null) : Sequence<Node> {
    override fun iterator() = Imp()

    private inner class Imp() : Iterator<Node> {
        val iteratorDequeue = Deque<Iterator<Node>>()
        var childrenIterator : Iterator<Node>
        var next: Node? = null

        init {
            val firstIterator = groupNode.children.iterator()
            iteratorDequeue.addBack(firstIterator)
            childrenIterator = firstIterator
        }

        override fun hasNext(): Boolean {
            if( next != null) return true

            while (iteratorDequeue.any()) {
                spin()
                if( next != null) return true
            }
            return false
        }

        private fun spin() {
            if( childrenIterator.hasNext()) {
                val node = childrenIterator.next()
                if( filter?.invoke(node) ?: true) {
                    next = node
                    if( node is GroupNode) {
                        val newIter = node.children.iterator()
                        iteratorDequeue.addBack(newIter)
                        childrenIterator = newIter
                    }
                }
            }
            else {
                iteratorDequeue.popBack()
                childrenIterator = iteratorDequeue.peekBack() ?: childrenIterator
            }
        }

        override fun next(): Node {
            var node = next
            while (node == null) {
                if(!hasNext()) throw IndexOutOfBoundsException()
                node = next
            }
            next = null
            return node!!
        }
    }

}