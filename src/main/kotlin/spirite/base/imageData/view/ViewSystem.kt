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
    fun get(node: Node, viewIndex: Int? = null) : NodeViewProperties
    fun set(node: Node, newProperties: NodeViewProperties, viewIndex: Int? = null)
    fun getCurrentNode(viewIndex: Int) : Node?
    fun setCurrentNode(viewIndex: Int, node: Node?)

    var numActiveViews: Int
    val viewBind : Bindable<Int>
    var view : Int
    val currentNodeBind : Bindable<Node?>
    var currentNode: Node?

    fun resetOtherViews()

    val animScrollViewModule : IAnimScrollViewModule
}

class ViewSystem(
    private val _undoEngine : IUndoEngine) : IViewSystem
{
    override val animScrollViewModule: IAnimScrollViewModule = AnimScrollViewModule( this,_undoEngine)

    private val _viewMapMap = mutableMapOf<Int, MutableMap<Node, NodeViewProperties>>()
    private val _selectedNodeMap = mutableMapOf<Int, Node?>()

    override var numActiveViews = 3

    override val viewBind = Bindable<Int>(0)
    override var view: Int by viewBind

    private fun getViewMap(i: Int) : MutableMap<Node,NodeViewProperties> {
        val existing = _viewMapMap[i]
        if( existing != null)
            return existing

        val current = _viewMapMap[view]
            ?: return mutableMapOf<Node,NodeViewProperties>().also { _viewMapMap[i] = it }

        val newMap = current.toMutableMap()
        _viewMapMap[i] = newMap
        return newMap
    }

    override val currentNodeBind = Bindable<Node?>(null)
            .also { it.addObserver { new, _ -> _selectedNodeMap[view] = new } }
    override var currentNode by currentNodeBind

    override fun get(node: Node, viewIndex: Int?) =
        getViewMap( viewIndex ?: view)[node] ?: NodeViewProperties()


    override fun set(node: Node, newProperties: NodeViewProperties, viewIndex: Int?) {
        val viewIndex = viewIndex ?: view
        val current = get(node, viewIndex)

        if( current == newProperties) return

        val action = when {
            current.visible == newProperties.visible && current.method == newProperties.method -> when {
                current.alpha == newProperties.alpha ->
                    NodePositionChangeAction(node, current.ox, current.oy, newProperties.ox, newProperties.oy, viewIndex)
                current.ox == newProperties.ox && current.oy == newProperties.oy ->
                    NodeAlphaChangeAction(node, current.alpha, newProperties.alpha, viewIndex)
                else -> GenericNodeViewPropertyAction(node, current, newProperties, viewIndex)
            }
            else -> GenericNodeViewPropertyAction(node, current, newProperties, viewIndex)
        }
        _undoEngine.performAndStore(action)
    }

    override fun getCurrentNode(viewIndex: Int): Node? {
        return _selectedNodeMap[viewIndex]
    }

    override fun setCurrentNode(viewIndex: Int, node: Node?) {
        if( viewIndex == view)
            currentNode = node
        else
            _selectedNodeMap[viewIndex] = node
    }

    override fun resetOtherViews() {
        val mainMap = getViewMap(view)

        _viewMapMap.keys.asSequence()
                .filter { it != view }
                .forEach { _viewMapMap[it] = mainMap.toMutableMap() }
    }



    // regionBindings
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
    // endregion

    private inner class GenericNodeViewPropertyAction(
        val node: Node,
        val oldViewProperties: NodeViewProperties,
        val newViewProperties: NodeViewProperties,
        val viewIndex: Int)
        : NullAction()
    {
        override val description: String get() = "Change GroupNode View Settings"

        override fun performAction() {
            getViewMap(viewIndex) [node] = newViewProperties
            if(viewIndex == view)
                node.triggerChange()
        }

        override fun undoAction() {
            getViewMap(viewIndex) [node] = oldViewProperties
            if(viewIndex == view)
                node.triggerChange()
        }
    }

    private inner class NodeAlphaChangeAction(
        val node: Node,
        val orgAlpha: Float,
        var newAlpha: Float,
        val viewIndex: Int)
        :NullAction(), StackableAction
    {
        override val description: String get() = "Change GroupNode Alpha"

        override fun performAction() {
            getViewMap(viewIndex)[node] = get(node).copy(alpha = newAlpha)
            if(viewIndex == view)
                node.triggerChange()
        }

        override fun undoAction() {
            getViewMap(viewIndex)[node] = get(node).copy(alpha = orgAlpha)
            if( viewIndex == view)
                node.triggerChange()
        }

        override fun canStack(other: UndoableAction) = (other is NodeAlphaChangeAction)
                && other.viewIndex == viewIndex
                && other.node == node
        override fun stackNewAction(other: UndoableAction) {newAlpha = (other as NodeAlphaChangeAction).newAlpha }
    }

    private inner class NodePositionChangeAction(
        val node: Node,
        val orgX: Int, val orgY: Int,
        var newX: Int, var newY: Int,
        val viewIndex: Int)
        :NullAction(), StackableAction
    {
        override val description: String get() = "Change GroupNode Position"

        override fun performAction() {
            getViewMap(viewIndex) [node] = get(node).copy(ox = newX, oy = newY)
            if( viewIndex == view)
                node.triggerChange()
        }

        override fun undoAction() {
            getViewMap(viewIndex) [node] = get(node).copy(ox = orgX, oy = orgY)
            if( viewIndex == view)
                node.triggerChange()
        }

        override fun canStack(other: UndoableAction) = (other is NodePositionChangeAction)
                && other.node == node
                && other.viewIndex == viewIndex

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