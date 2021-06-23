package spirite.base.imageData.groupTree

import rb.extendo.extensions.then
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.MutableTransformF
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.view.IViewSystem
import spirite.sguiHybrid.MDebug
import kotlin.reflect.KProperty

abstract class Node(
    val tree: GroupTree,
    protected val _trigger : GroupTree.IGroupTreeTrigger,
    protected val _viewSystem : IViewSystem,
    protected val _undoEngine : IUndoEngine?,
    parent: GroupNode?,
    name: String)
{
    fun triggerChange( renderChanged: Boolean = true) = _trigger.triggerNodeAttributeChanged(this, renderChanged)
    // region Properties
    private var view get() = _viewSystem.get(this) ; set(value) {_viewSystem.set(this, value)}

    var visible get() = view.visible ; set(value)  {view = view.copy(visible = value)}
    var alpha get() = view.alpha ; set(value) {view = view.copy(alpha = value)}
    var method  get() = view.method ; set(value) { view = view.copy(method = value)}
    var x get() = view.ox ; set(value) {view = view.copy(ox = value)}
    var y get() = view.oy ; set(value) {view = view.copy(oy = value)}

    var expanded : Boolean by NodePropertyDelegate( true, _undoEngine,"SmallExpanded/Contracted ${tree.treeDescription} GroupNode", false)
    var name : String by NodePropertyDelegate( name, _undoEngine,"Changed ${tree.treeDescription} GroupNode's Name", false)
    val isVisible : Boolean get() = visible && alpha > 0f

    val tNodeToContext get() = ImmutableTransformF.Translation(x + 0f, y + 0f)
    val tNodeToRoot : MutableTransformF
        get() =
        ancestors.foldRight(tNodeToContext.toMutable()) {node,trans->
            trans.also { it.preConcatenate(node.tNodeToContext) }
        }
    // endregion

    // region Delegates
    inner class NodePropertyDelegate<T>(
        defaultValue : T,
        val undoEngine: IUndoEngine?,
        val changeDescription: String,
        val isRenderPropert: Boolean = true)
    {
        var field = defaultValue
            set(value) {
                field = value
                _trigger.triggerNodeAttributeChanged(this@Node, isRenderPropert)
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
        while( node != tree.root) {
            ++d
            if( node == null) return -1
            node = node.parent
        }
        return d
    }

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
                if( predicate.invoke(it)) list.add(it)
                if(it is GroupNode && (checkChildren?.invoke(it) ?: true)) sub(it.children)
            }
        }
        sub(listOf(this))
        return list
    }
    fun getAllNodesSuchThatSeq(predicate : (Node) -> Boolean, checkChildren : ((GroupNode) -> Boolean)? = null) : Sequence<Node> {
        return when {
            predicate.invoke(this) -> sequenceOf(this)
            else -> emptySequence()
        }.then(when {
            this is GroupNode && checkChildren?.invoke(this) ?: true -> children.asSequence().flatMap { getAllNodesSuchThatSeq(predicate, checkChildren) }
            else -> emptySequence()
        })
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
            MDebug.handleError(MDebug.ErrorType.STRUCTURAL, "Group Tree malformation (Not child of own parent).")
            return null
        }

        if( i == children.size-1) return null
        return children[i+1]
    }

    val previousNode: Node? get() {
        val children = parent?.children ?: return null
        val i = children.indexOf( this)

        if( i == -1) {
            MDebug.handleError(MDebug.ErrorType.STRUCTURAL, "Group Tree malformation (Not child of own parent).")
            return null
        }

        if( i == 0) return null
        return children[i-1]
    }

    fun delete() {
        val p = parent
        if( p == null) {
            MDebug.handleError(
                MDebug.ErrorType.STRUCTURAL,
                "Tried to Delete GroupNode that has no parent (root node or floating node)."
            )
            return
        }
        p.remove(this)
    }
}