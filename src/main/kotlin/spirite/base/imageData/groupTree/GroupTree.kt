package spirite.base.imageData.groupTree

import rb.extendo.dataStructures.Dequeue
import spirite.base.util.binding.Bindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.graphics.RenderMethod
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction
import spirite.base.util.delegates.UndoableDelegate
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.MutableTransformF
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL
import kotlin.reflect.KProperty

/**
 * The GroupTree is an abstract Container Structure used by several components to organize ImageData in a hierarchical
 * tree structure.
 */
open class GroupTree( val undoEngine: IUndoEngine?)
{
    val root = GroupNode(null, "ROOT")
    open val treeDescription = "Abstract Group Tree"

    var selectedNodeBind = Bindable<Node?>(null)
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
        fun nodePropertiesChanged( node: Node, renderChanged: Boolean)
    }
    class TreeChangeEvent( val changedNodes : Set<Node>)
    val treeObservable : IObservable<TreeObserver> get() = _treeObs
    private val _treeObs = Observable<TreeObserver>()

    protected fun triggerChange(evt : TreeChangeEvent) {_treeObs.trigger { it.treeStructureChanged(evt) }}
    protected fun triggerNodeAttributeChanged( node:  Node, renderChanged: Boolean) {_treeObs.trigger { it.nodePropertiesChanged(node, renderChanged) }}
    // endregion

    abstract inner class Node( parent: GroupNode?, name: String) {
        // Properties
        var visible by NodePropertyDelegate(true, undoEngine, "Changed $treeDescription Node's Visibility")
        var alpha by NodePropertyDelegate(1.0f, undoEngine, "Changed $treeDescription Node's Alpha")
        var method by NodePropertyDelegate(RenderMethod(), undoEngine, "Changed $treeDescription Node's Method")

        private val pos = NodePosition()
        var x
            get() = pos.x
            set(value) {pos.changeX(value)}
        var y
            get() = pos.y
            set(value) {pos.changeY(value)}
        var expanded : Boolean by NodePropertyDelegate( true, undoEngine,"Expanded/Contracted $treeDescription Node", false)
        var name : String by NodePropertyDelegate( name, undoEngine,"Changed $treeDescription Node's Name", false)

        val isVisible : Boolean get() = visible && alpha > 0f

        val tNodeToContext get() = ImmutableTransformF.Translation(x+0f, y+0f)
        val tNodeToRoot : MutableTransformF get() =
            ancestors.foldRight(tNodeToContext.toMutable()) {node,trans->
                trans.also { it.preConcatenate(node.tNodeToContext) }
            }

        // region Delegates
        private inner class NodePosition {
            var x = 0
                private set(value) {
                    field = value
                    triggerNodeAttributeChanged( this@Node, true)
                }
            var y = 0 ; private set

            fun changeX( new: Int) {
                if( undoEngine == null) x = new
                else if( new != x) undoEngine.performAndStore(NodePositionChangeAction(x,y,new,y))
            }
            fun changeY( new: Int) {
                if( undoEngine == null) y = new
                else if( new != y) undoEngine.performAndStore(NodePositionChangeAction(x,y,x,new))
            }


            inner class NodePositionChangeAction(val oldX: Int, val oldY: Int, var newX: Int, var newY: Int)
                : NullAction(), StackableAction
            {
                override val description: String get() = "Changed ${treeDescription} Node's Position"
                val context : Node get() = this@Node

                override fun performAction() {
                    x = newX
                    y = newY
                }

                override fun undoAction() {
                    x = oldX
                    y = oldY
                }

                override fun canStack(other: UndoableAction) = other is NodePositionChangeAction && other.context == context
                override fun stackNewAction(other: UndoableAction) {
                    val other = other as NodePositionChangeAction
                    newX = other.newX
                    newY = other.newY
                }
            }
        }

        inner class NodePropertyDelegate<T>(
                defaultValue : T,
                val undoEngine: IUndoEngine?,
                val changeDescription: String,
                val isRenderPropert: Boolean = true)
        {
            var field = defaultValue
                set(value) {
                    field = value
                    triggerNodeAttributeChanged(this@Node, isRenderPropert)
                }

            operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

            operator fun setValue(thisRef: Any, prop: KProperty<*>, value: T) {
                if (undoEngine == null) {
                    field = value
                } else if (field != value) {
                    val oldValue = field
                    val newValue = value
                    undoEngine.performAndStore(object : NullAction() {
                        override val description: String get() = changeDescription
                        override fun performAction() {
                            field = newValue
                        }

                        override fun undoAction() {
                            field = oldValue
                        }
                    })
                }
            }
        }
        // endregion

        // region Structure
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
        // endregion

        abstract val imageDependencies : Collection<MediumHandle>

        val descendants get() = getAllNodesSuchThat ({true})

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

        val ancestors : List<GroupNode> get() {
            val list = mutableListOf<GroupNode>()
            var p = this.parent
            while( p != null) {
                list.add(p)
                p = p.parent
            }
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
            val oldParent = toMove.parent
            _remove( toMove, false)
            newParent._add( toMove, newBefore, false)
            triggerChange(TreeChangeEvent(setOf(toMove, newParent, oldParent ?: toMove)))
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
                triggerChange(TreeChangeEvent(setOf(toAdd, this)))
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
        private fun _remove( toRemove: Node, trigger: Boolean = true) {
            val parent = toRemove.parent
            _children.remove( toRemove)
            triggerChange(TreeChangeEvent(setOf(toRemove, parent ?: toRemove)))
        }
    }

    inner class LayerNode(
            parent: GroupNode?,
            name: String,
            val layer: Layer)
        : Node(parent, name)
    {
        override val imageDependencies: Collection<MediumHandle> get() = layer.imageDependencies

        fun getDrawList() : List<TransformedHandle> {
            val transform = tNodeToContext
            return layer.getDrawList().map { it.stack(transform) }
        }
    }
}

fun GroupNode.traverse() : Sequence<Node> = GroupNodeTraversalSequence(this)
fun GroupNode.traverse(filter: (Node) -> Boolean) : Sequence<Node> = GroupNodeTraversalSequence(this, filter)

private class GroupNodeTraversalSequence(
        val groupNode: GroupNode,
        val filter: ((Node)->Boolean)? = null) : Sequence<Node> {
    override fun iterator() = Imp()

    private inner class Imp() : Iterator<Node> {
        val iteratorDequeue = Dequeue<Iterator<Node>>()
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
            return node
        }
    }

}