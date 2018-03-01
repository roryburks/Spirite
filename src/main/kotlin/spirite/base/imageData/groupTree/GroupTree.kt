package spirite.base.imageData.groupTree

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.graphics.RenderProperties
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.util.delegates.UndoableDelegate
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import kotlin.reflect.KProperty

/**
 * The GroupTree is an abstract Container Structure used by several components to organize ImageData in a hierarchical
 * tree structure.
 */
open class GroupTree( val undoEngine: IUndoEngine?)
{
    val root = GroupNode(null, "ROOT")
    open val treeDescription = "Abstract Group Tree"

    // region Selection (observable)
    var selectedNode : Node? = null
        set(value) {
            if( field != value) {
                val old = field
                field = value
                _selectionObserver.trigger { it.selectionChanged(value, old)}
            }
        }

    interface NodeSelectionObserver {
        fun selectionChanged( newSelection: Node?, oldSelection: Node?)
    }
    val selectionObserver: IObservable<NodeSelectionObserver> get() = _selectionObserver
    val _selectionObserver = Observable<NodeSelectionObserver>()
    // endregion

    abstract inner class Node( parent: GroupNode?, name: String) {
        // Properties
        var render : RenderProperties by UndoableDelegate(RenderProperties(), undoEngine, "Changed $treeDescription Node's Render Settings")
        var x : Int by UndoableDelegate(0, undoEngine,"Changed $treeDescription Node's X offset")
        var y : Int by UndoableDelegate(0, undoEngine,"Changed $treeDescription Node's Y offset")
        var expanded : Boolean by UndoableDelegate( true, undoEngine,"Expanded/Contracted $treeDescription Node")
        var name : String by UndoableDelegate( name, undoEngine,"Changed $treeDescription Node's Name")

        val tNodeToContext get() = Transform.TranslationMatrix(x+0f, y+0f)

        // Structure
        var parent = parent ; internal set

        val depth : Int get() {
            // Note: non-looping integrity is handled by the insert/change parent functionality
            var node : Node? = this
            var d = 0
            while( node != root) {
                ++d
                if( node == null) return -1
                node = node.parent
            }
            return d
        }

        val tree get() = this@GroupTree

        fun getDepthFrom( ancestor: Node) : Int {
            tailrec fun sub(nodeToTest: Node?, layer: Int = 0): Int = when (nodeToTest) {
                null -> -1
                ancestor -> layer
                else -> sub(nodeToTest.parent, layer + 1)
            }
            return sub(this)
        }

        fun isChildOf( other: Node) : Boolean{
            tailrec fun sub(nodeCheck: Node?) : Boolean = when( nodeCheck) {
                other -> true
                null -> false
                else -> sub(nodeCheck.parent)
            }
            return sub(this)
        }

        fun getLayerNodes(): List<LayerNode> {
            val list = mutableListOf<LayerNode>()

            fun sub(nodes: List<Node>) {
                nodes.forEach {
                    when( it) {
                        is LayerNode -> list.add(it)
                        is GroupNode -> sub(it.children)
                    }
                }
            }
            sub(listOf(this))
            return list
        }

        abstract val imageDependencies : Collection<MediumHandle>

        /**
         * Gets ancestors of the current node such that a certain predicate is true.
         * @param checkChildren If Null, will always check children. */
        fun getAllNodesSuchThat(predicate : (Node) -> Boolean, checkChildren : ((GroupNode) -> Boolean)? = null) : List<Node> {
            val list = mutableListOf<Node>()

            fun sub(nodes: List<Node>) {
                nodes.forEach {
                    when {
                        predicate.invoke(it) -> list.add(it)
                        (it is GroupNode && (checkChildren?.invoke(it) ?: true)) -> sub(it.children)
                    }
                }
            }
            sub(listOf(this))
            return list
        }

        val nextNode: Node? get() {
            val children = parent?.children ?: return null
            val i = children.indexOf( this)

            if( i == -1) {
                MDebug.handleError(ErrorType.STRUCTURAL, "Group Tree malformation (Not child of own parent).")
                return null
            }

            if( i == children.size-1) return null
            return children[i+1]
        }

        val previousNode: Node? get() {
            val children = parent?.children ?: return null
            val i = children.indexOf( this)

            if( i == -1) {
                MDebug.handleError(ErrorType.STRUCTURAL, "Group Tree malformation (Not child of own parent).")
                return null
            }

            if( i == 0) return null
            return children[i-1]
        }

        fun delete() {
            val p = parent
            if( p == null) {
                MDebug.handleError(ErrorType.STRUCTURAL, "Tried to Delete Node that has no parent (root node or floating node).")
                return
            }
            p.remove(this)
        }
    }

    inner class GroupNode(parent: GroupNode?, name: String) : Node(parent, name) {
        override val imageDependencies : Collection<MediumHandle> get() {
            val set = mutableSetOf<MediumHandle>()
            children.forEach { set.addAll(it.imageDependencies) }
            return set
        }

        val children: List<Node> get() = _children
        var flatenned : Boolean by UndoableDelegate(false, undoEngine, "Toggled Group Node Flattened")

        private val _children = mutableListOf<Node>()

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

        internal fun move( toMove: Node, newParent: GroupNode, newBefore: Node?) {
            if( undoEngine == null) _move(toMove, newParent, newBefore)
            else {
                val oldParent = this
                val oldBefore = toMove.nextNode

                undoEngine.performAndStore(object: NullAction() {
                    override val description: String get() = "Add Node to $treeDescription "
                    override fun performAction() = _move(toMove, newParent, newBefore)
                    override fun undoAction() = newParent._move(toMove, oldParent, oldBefore)
                })
            }

        }
        private fun _move( toMove: Node, newParent: GroupNode, newBefore: Node?) {
            _remove( toMove)
            newParent._add( toMove, newBefore)
        }

        internal fun add(toAdd: Node, before: Node?) {
            if( undoEngine == null) _add(toAdd, before)
            else {
                undoEngine.performAndStore(object: NullAction() {
                    override val description: String get() = "Add Node to $treeDescription "

                    override fun performAction() = _add(toAdd, before)

                    override fun undoAction() {
                        _remove(toAdd)
                        if( selectedNode == toAdd)
                            selectedNode = null
                    }
                })
            }
        }
        private fun _add(toAdd: Node, before: Node?) {
            // if( toAdd.tree != this@GroupTree)
            // Todo

            val index = _children.indexOf(before)

            when( index) {
                -1 -> _children.add(toAdd)
                else -> _children.add(index, toAdd)
            }
            toAdd.parent = this
        }


        internal fun remove( toRemove: Node) {
            if( undoEngine == null) _remove(toRemove)
            else {
                val before = toRemove.nextNode
                undoEngine.performAndStore(object: NullAction() {
                    override val description: String get() = "Remove Node from $treeDescription "

                    override fun performAction() {
                        _remove(toRemove)
                        if( selectedNode == toRemove)
                            selectedNode = null
                    }

                    override fun undoAction() = _add(toRemove, before)
                })
            }
        }
        private fun _remove( toRemove: Node) {
            _children.remove( toRemove)
        }
    }

    inner class LayerNode(
            parent: GroupNode?,
            name: String,
            val layer: Layer)
        : Node(parent, name)
    {
        override val imageDependencies: Collection<MediumHandle> get() = layer.imageDependencies
    }
}