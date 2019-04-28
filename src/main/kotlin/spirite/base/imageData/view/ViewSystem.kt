package spirite.base.imageData.view

import rb.owl.bindable.Bindable
import spirite.base.graphics.RenderMethod
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.NullAction
import spirite.base.imageData.undo.StackableAction
import spirite.base.imageData.undo.UndoableAction

interface IViewSystem
{
    fun get(node: Node) : NodeViewProperties
    fun set(node: Node, newProperties: NodeViewProperties)

    var view : Int
    val currentNodeBind : Bindable<Node?>
    var currentNode: Node?
}

class ViewSystem(private val _undoEngine : IUndoEngine) : IViewSystem
{
    private val _viewMap get() = _viewMapMap[_currentViewMap]
            ?: (mutableMapOf<Node,NodeViewProperties>().also { _viewMapMap[_currentViewMap] = it })
    private var _currentViewMap = 0
    private val _viewMapMap = mutableMapOf<Int, MutableMap<Node, NodeViewProperties>>()
    private val _selectedNodeMap = mutableMapOf<Int, Node?>()

    override val currentNodeBind = Bindable<Node?>(null)
    override var currentNode by currentNodeBind

    override var view: Int
        get() = _currentViewMap
        set(value) {
            if( value == _currentViewMap) return

            val effectedNodes = _viewMap.keys.toHashSet()
                    .union(_viewMapMap[value]?.keys ?: emptySet())
            if( _viewMapMap[value] == null) {
                _viewMapMap[_currentViewMap]?.also { _viewMapMap[value] = it.toMutableMap() }
                _selectedNodeMap[value] = _selectedNodeMap[_currentViewMap]
            }
            _currentViewMap = value
            effectedNodes.forEach { it.triggerChange() }
            currentNode = _selectedNodeMap[value]
        }

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



    private inner class GenericNodeViewPropertyAction(
            val node: Node,
            val oldViewProperties: NodeViewProperties,
            val newViewProperties: NodeViewProperties)
        : NullAction()
    {
        override val description: String get() = "Change Node View Settings"

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
        override val description: String get() = "Change Node Alpha"

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
        override val description: String get() = "Change Node Position"

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