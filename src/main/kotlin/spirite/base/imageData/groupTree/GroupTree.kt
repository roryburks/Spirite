package spirite.base.imageData.groupTree

import spirite.base.graphics.RenderProperties
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.util.delegates.UndoableDelegate
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
    var selectedNode : Node? = null

    fun parentOfContext( context: Node?) = when(context) {
        null -> root
        is GroupNode -> context
        else -> context.parent ?: root
    }

    fun beforeContext( context: Node?) = when(context) {
        null, is GroupNode -> null
        else -> context
    }


    abstract inner class Node(
            parent: GroupNode?,
            name: String)
    {
        // Properties
        var render : RenderProperties by UndoableDelegate(RenderProperties(), undoEngine, "Changed Node's Render Settings")
        var x : Int by UndoableDelegate(0, undoEngine,"Changed Node's X offset")
        var y : Int by UndoableDelegate(0, undoEngine,"Changed Node's Y offset")
        var expanded : Boolean by UndoableDelegate( true, undoEngine,"Expanded/Contracted Node")
        var name : String by UndoableDelegate( name, undoEngine,"Changed Node's Name")

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
            tailrec fun gdfTR(ancestor: Node, nodeToTest: Node? = null, layer: Int = 0): Int = when (nodeToTest) {
                null -> -1
                ancestor -> layer
                else -> gdfTR(ancestor, nodeToTest.parent, layer + 1)
            }
            return gdfTR(ancestor)
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

    inner class GroupNode: Node {
        constructor(parent: GroupNode?, name: String) : super(parent, name)

        val children: List<Node> get() = _children
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

        fun add(toAdd: Node, before: Node?) {
            if( undoEngine == null) _add(toAdd, before)
            else {
                undoEngine.performAndStore(object: NullAction() {
                    override val description: String get() = "Add Node"

                    override fun performAction() {
                        _add(toAdd, before)
                    }

                    override fun undoAction() {
                        _remove(toAdd)
                        if( selectedNode == toAdd)
                            selectedNode = null
                    }
                })
            }
        }
        fun remove( toRemove: Node) {
            if( undoEngine == null) _remove(toRemove)
            else {
                val before = toRemove.nextNode
                undoEngine.performAndStore(object: NullAction() {
                    override val description: String get() = "Add Node"

                    override fun performAction() {
                        _remove(toRemove)
                        if( selectedNode == toRemove)
                            selectedNode = null
                    }

                    override fun undoAction() {
                        _add(toRemove, before)
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
        private fun _remove( toRemove: Node) {
            _children.remove( toRemove)
        }
    }

    inner class LayerNode(
            parent: GroupNode?,
            name: String,
            val layer: Layer) : Node(parent, name)
}