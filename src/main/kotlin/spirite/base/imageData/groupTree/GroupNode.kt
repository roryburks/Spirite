package spirite.base.imageData.groupTree

import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.UndoableChangeDelegate
import spirite.base.imageData.view.IViewSystem

class GroupNode(
    tree: GroupTree,
    trigger : GroupTree.IGroupTreeTrigger,
    viewSystem: IViewSystem,
    undoEngine: IUndoEngine?,
    parent: GroupNode?,
    name: String) : Node(tree, trigger, viewSystem, undoEngine, parent, name)
{
    private val _children = mutableListOf<Node>()

    val children: List<Node> get() = _children
    var flattened : Boolean by UndoableChangeDelegate(false, undoEngine, "Toggled Group GroupNode Flattened") {
        triggerChange(renderChanged = true)
    }

    override val imageDependencies : Collection<MediumHandle> get() {
        val set = mutableSetOf<MediumHandle>()
        children.forEach { set.addAll(it.imageDependencies) }
        return set
    }

    fun getAllAncestors() : List<Node>{
        val list = mutableListOf<Node>()
        fun sub(node: GroupNode) {
            node.children.forEach {
                list.add(it)
                if( it is GroupNode) sub(it)
            }
        }
        sub(this)
        return list
    }

    internal fun move(toMove: Node, newParent: GroupNode, newBefore: Node?) {
        if( _undoEngine == null) _move(toMove, newParent, newBefore)
        else {
            val oldParent = this
            val oldBefore = toMove.nextNode

            _undoEngine.performAndStore(object: NullAction() {
                override val description: String get() = "Add GroupNode to ${tree.treeDescription} "
                override fun performAction() = _move(toMove, newParent, newBefore)
                override fun undoAction() = newParent._move(toMove, oldParent, oldBefore)
            })
        }

    }
    private fun _move(toMove: Node, newParent: GroupNode, newBefore: Node?) {
        val oldParent = toMove.parent
        _remove( toMove, false)
        newParent._add( toMove, newBefore, false)
        _trigger.triggerChange(GroupTree.TreeChangeEvent(setOf(toMove, newParent, oldParent ?: toMove)))
    }

    internal fun add(toAdd: Node, before: Node?) {
        if( _undoEngine == null) _add(toAdd, before)
        else {
            _undoEngine.performAndStore(object: NullAction() {
                override val description: String get() = "Add GroupNode to ${tree.treeDescription} "

                override fun performAction() = _add(toAdd, before)

                override fun undoAction() {
                    _remove(toAdd)
                    if( tree.selectedNode == toAdd)
                        tree.selectedNode = null
                }
            })
        }
    }
    private fun _add(toAdd: Node, before: Node?, trigger: Boolean = true) {
        // if( toAdd.tree != this@GroupTree)
        // Todo

        val index = _children.indexOf(before)

        when( index) {
            -1 -> _children.add(toAdd)
            else -> _children.add(index, toAdd)
        }
        toAdd.parent = this
        if( trigger)
            _trigger.triggerChange(GroupTree.TreeChangeEvent(setOf(toAdd, this)))
    }


    internal fun remove( toRemove: Node) {
        if( _undoEngine == null) _remove(toRemove)
        else {
            val before = toRemove.nextNode
            _undoEngine.performAndStore(object: NullAction() {
                override val description: String get() = "Remove GroupNode from ${tree.treeDescription} "

                override fun performAction() {
                    _remove(toRemove)
                    if( tree.selectedNode == toRemove)
                        tree.selectedNode = null
                }

                override fun undoAction() = _add(toRemove, before)
            })
        }
    }
    private fun _remove(toRemove: Node, trigger: Boolean = true) {
        val parent = toRemove.parent
        _children.remove( toRemove)
        _trigger.triggerChange(GroupTree.TreeChangeEvent(setOf(toRemove, parent ?: toRemove)))
    }
}