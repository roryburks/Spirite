package spirite.base.imageData.groupTree

import spirite.base.imageData.undo.IUndoEngine
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

open class MovableGroupTree(undoEngine: IUndoEngine?) : GroupTree(undoEngine) {

    fun parentOfContext( context: Node?) = when(context) {
        null -> root
        is GroupNode -> context
        else -> context.parent ?: root
    }

    fun beforeContext( context: Node?) = when(context) {
        null, is GroupNode -> null
        else -> context
    }

    fun addGroupNode( contextNode: Node?, name: String) : GroupNode {
        val new = GroupNode(null, name)
        insertNode(contextNode, new)
        return new
    }

    fun moveAbove( nodeToMove: Node, nodeAbove: Node) {
        val newParent = nodeAbove.parent
        if( newParent == null ) {
            MDebug.handleWarning(STRUCTURAL, "Attempted to move a node after a root or detatched node")
            return
        }
        moveNode(nodeToMove, newParent, nodeAbove)
    }

    fun moveBelow( nodeToMove: Node, nodeUnder: Node) {
        val newParent = nodeUnder.parent
        if( newParent == null) {
            MDebug.handleWarning(STRUCTURAL, "Attempted to move a node before a root or detatched node")
            return
        }
        moveNode(nodeToMove, newParent, nodeUnder.nextNode)
    }

    fun moveInto( nodeToMove: Node, nodeInto: GroupNode, top: Boolean = false) {
        moveNode( nodeToMove, nodeInto, if( top) nodeInto.children.firstOrNull() else null)
    }

    protected fun insertNode(contextNode: Node?, nodeToInsert: Node) {
        val parent = parentOfContext(contextNode)
        val before = beforeContext(contextNode)
        parent.add(nodeToInsert, before)
    }

    protected fun moveNode( nodeToMove: Node, newParent: GroupNode, newBefore: Node?) {
        val parent = nodeToMove.parent
        if( parent == null) {
            MDebug.handleWarning(STRUCTURAL, "Attempted to move a node that isn't in the tree")
            return
        }
        if( parent.isChildOf( nodeToMove)){
            MDebug.handleWarning(STRUCTURAL, "Attempted to move a Node into its child.")
            return
        }

        parent.move( nodeToMove, newParent, newBefore)
    }
}