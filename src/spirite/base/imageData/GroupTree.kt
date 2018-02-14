package spirite.base.imageData

import com.sun.org.apache.xpath.internal.operations.Bool
import spirite.base.graphics.RenderProperties
import spirite.base.imageData.layers.Layer
import spirite.base.util.delegates.OnChangeDelegate
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import kotlin.reflect.KProperty

/**
 * The GroupTree is an abstract Container Structure used by several components to organize ImageData in a hierarchical
 * tree structure.
 */
class GroupTree(
        val context: IImageWorkspace
) {
    val root = GroupNode()


    //interface Node

    abstract inner class Node(
            parent: GroupNode?,
            name: String
    ) {

        // Properties
        var render : RenderProperties by OnChangeDelegate(RenderProperties(), {})
        var x : Int by OnChangeDelegate(0, {})
        var y : Int by OnChangeDelegate(0, {})
        var expanded : Boolean by OnChangeDelegate( true, {})
        var name : String by OnChangeDelegate( name, {})

        // Structure
        var parent = parent

        val depth : Int get() {
            // Note: non-looping integrity is handled by the insert/change parent functionality
            var node : GroupTree.Node? = this
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
            var node : GroupTree.Node? = this
            var d = 0
            while( node != ancestor) {
                ++d
                if( node == null) return -1
                node = node.parent
            }
            return d
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
        fun getAllNodesSuchThat( predicate : (Node) -> Boolean, checkChildren : ((GroupNode) -> Boolean)? = null) : List<Node> {
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
        constructor( parent: GroupNode?, name: String) : super(parent, name)
        internal constructor() : super(null, "ROOT")

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

        fun add(  toAdd: Node, before: Node?) {
            // if( toAdd.tree != this@GroupTree)
            // Todo

            val index = _children.indexOf(before)

            when( index) {
                -1 -> _children.add(toAdd)
                else -> _children.add(index, toAdd)
            }
            toAdd.parent = this
        }

        fun remove( toRemove: Node) {
            _children.remove( toRemove)
        }
    }

    inner class LayerNode(
            parent: GroupNode?,
            name: String,
            val layer: Layer) : Node(parent, name)
}