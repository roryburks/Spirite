package spirite.base.imageData.view

import rb.glow.gle.RenderMethod
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction

interface IViewSystem
{
    fun get(node: Node) : NodeViewProperties
    fun set(node: Node, newProperties: NodeViewProperties)

    var numActiveViews: Int
    val viewBind : Bindable<Int>
    var view : Int
    val currentNodeBind : Bindable<Node?>
    var currentNode: Node?

    fun resetOtherViews()
}

class ViewSystem(private val _undoEngine : IUndoEngine) : IViewSystem
{
    private val _viewMap get() = _viewMapMap[view]
            ?: (mutableMapOf<Node,NodeViewProperties>().also { _viewMapMap[view] = it })
    private val _viewMapMap = mutableMapOf<Int, MutableMap<Node, NodeViewProperties>>()
    private val _selectedNodeMap = mutableMapOf<Int, Node?>()

    override var numActiveViews = 3

    override val viewBind = Bindable<Int>(0)
    override var view: Int by viewBind
    private val _viewChangeK = viewBind.addObserver { new, old ->
        if( new == old) return@addObserver

        val effectedNodes = (_viewMapMap[old]?.keys?.toHashSet() ?: emptySet<Node>())
                .union(_viewMapMap[new]?.keys ?: emptySet())
        if( _viewMapMap[new] == null) {
            _viewMapMap[old]?.also { _viewMapMap[new] = it.toMutableMap() }
            _selectedNodeMap[new] = _selectedNodeMap[old]
        }
        effectedNodes.forEach { it.triggerChange() }
        currentNode = _selectedNodeMap[new]
    }

    override val currentNodeBind = Bindable<Node?>(null)
            .also { it.addObserver { new, _ -> _selectedNodeMap[view] = new } }
    override var currentNode by currentNodeBind

    override fun get(node: Node) = _viewMap[node] ?: NodeViewProperties()


    override fun set(node: Node, newProperties: NodeViewProperties) {
        val current = get(node)

        if( current == newProperties) return

        val action = when {
            current.visible == newProperties.visible && current.method == newProperties.method -> when {
                current.alpha == newProperties.alpha ->
                    NodePositionChangeAction(node, current.ox, current.oy, newProperties.ox, newProperties.oy)
                current.ox == newProperties.ox && current.oy == newProperties.oy ->
                    NodeAlphaChangeAction(node, current.alpha, newProperties.alpha)
                else -> GenericNodeViewPropertyAction(node, current, newProperties)
            }
            else -> GenericNodeViewPropertyAction(node, current, newProperties)
        }
        _undoEngine.performAndStore(action)
    }

    override fun resetOtherViews() {
        val mainMap = _viewMap

        _viewMapMap.keys.asSequence()
                .filter { it != view }
                .forEach { _viewMapMap[it] = _viewMap.toMutableMap() }
    }



    private inner class GenericNodeViewPropertyAction(
        val node: Node,
        val oldViewProperties: NodeViewProperties,
        val newViewProperties: NodeViewProperties)
        : NullAction()
    {
        override val description: String get() = "Change GroupNode View Settings"

        override fun performAction() {
            _viewMap[node] = newViewProperties
            node.triggerChange()
        }

        override fun undoAction() {
            _viewMap[node] = oldViewProperties
            node.triggerChange()
        }
    }

    private inner class NodeAlphaChangeAction(
        val node: Node,
        val orgAlpha: Float,
        var newAlpha: Float)
        :NullAction(), StackableAction
    {
        override val description: String get() = "Change GroupNode Alpha"

        override fun performAction() {
            _viewMap[node] = get(node).copy(alpha = newAlpha)
            node.triggerChange()
        }

        override fun undoAction() {
            _viewMap[node] = get(node).copy(alpha = orgAlpha)
            node.triggerChange()
        }

        override fun canStack(other: UndoableAction) = other is NodeAlphaChangeAction
        override fun stackNewAction(other: UndoableAction) {newAlpha = (other as NodeAlphaChangeAction).newAlpha }
    }

    private inner class NodePositionChangeAction(
        val node: Node,
        val orgX: Int, val orgY: Int,
        var newX: Int, var newY: Int)
        :NullAction(), StackableAction
    {
        override val description: String get() = "Change GroupNode Position"

        override fun performAction() {
            _viewMap[node] = get(node).copy(ox = newX, oy = newY)
            node.triggerChange()
        }

        override fun undoAction() {
            _viewMap[node] = get(node).copy(ox = orgX, oy = orgY)
            node.triggerChange()
        }

        override fun canStack(other: UndoableAction) = other is NodePositionChangeAction

        override fun stackNewAction(other: UndoableAction) {
            other as NodePositionChangeAction
            newX = other.newX
            newY = other.newY
        }

    }
}

data class NodeViewProperties(
        val ox : Int = 0,
        val oy: Int = 0,
        val visible : Boolean = true,
        val alpha: Float = 1f,
        val method : RenderMethod = RenderMethod())
{
    val isVisible get() = visible && alpha != 0f
}