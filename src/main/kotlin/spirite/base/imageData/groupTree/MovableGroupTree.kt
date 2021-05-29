package spirite.base.imageData.groupTree

import spirite.sguiHybrid.MDebug
import spirite.sguiHybrid.MDebug.WarningType.STRUCTURAL
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.groupTree.PrimaryGroupTree.InsertBehavior
import spirite.base.imageData.groupTree.PrimaryGroupTree.InsertBehavior.*

open class MovableGroupTree(
        val workspace: MImageWorkspace) : GroupTree(workspace.undoEngine, workspace.viewSystem) {

    fun parentFromContext(context: Node?) = when(context) {
        null -> root
        is GroupNode -> context
        else -> context.parent ?: root
    }

    fun beforeFromContext(context: Node?) = when(context) {
        null, is GroupNode -> null
        else -> context
    }

    fun addGroupNode( contextNode: Node?, name: String, behavior: InsertBehavior? = null) : GroupNode {
        val new = GroupNode(null, name)
        insertNode(contextNode, new, behavior)
        return new
    }

    fun moveAbove( nodeToMove: Node, nodeAbove: Node) {
        if( nodeToMove.nextNode == nodeAbove) return

        val newParent = nodeAbove.parent
        if( newParent == null ) {
            MDebug.handleWarning(STRUCTURAL, "Attempted to move a node after a root or detatched node")
            return
        }
        moveNode(nodeToMove, newParent, nodeAbove)
    }

    fun moveBelow( nodeToMove: Node, nodeUnder: Node) {
        if( nodeToMove.previousNode == nodeUnder) return

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

    internal fun insertNode(contextNode: Node?, nodeToInsert: Node, behavior: InsertBehavior? = null) {
        val parent : GroupNode
        val before: Node?
        when( behavior) {
            Above -> {
                parent = nodeToInsert.parent ?: root
                before = nodeToInsert
            }
            Bellow -> {
                parent = nodeToInsert.parent ?: root
                before = nodeToInsert.nextNode
            }
            InsertBottom -> {
                parent = nodeToInsert as? GroupNode ?: nodeToInsert.parent ?: root
                before = null
            }
            InsertTop -> {
                parent = nodeToInsert as? GroupNode ?: nodeToInsert.parent ?: root
                before = parent.children.firstOrNull()
            }
            null -> {
                parent = parentFromContext(contextNode)
                before = beforeFromContext(contextNode)
            }
        }
        parent.add(nodeToInsert, before)
    }


    protected fun moveNode( nodeToMove: Node, newParent: GroupNode, newBefore: Node?) {
        val parent = nodeToMove.parent

        if( newParent == nodeToMove) return
        if( newParent.ancestors.contains(nodeToMove)) return
        if( parent == null) {
            MDebug.handleWarning(STRUCTURAL, "Attempted to move a node that isn't in the tree")
            return
        }

        parent.move( nodeToMove, newParent, newBefore)
    }
}