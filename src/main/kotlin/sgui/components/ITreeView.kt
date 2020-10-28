package sgui.components

import rb.glow.img.IImage
import rb.glow.img.NillImage
import rbJvm.glow.SColor
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import sgui.components.ITreeViewNonUI.ITreeNode
import sgui.components.ITreeViewNonUI.ITreeNodeAttributes
import sgui.transfer.ITransferObject
import sgui.transfer.StringTransferObject

interface ITreeViewNonUI<T>{
    var buildingPaused : Boolean
    var gapSize: Int
    var leftSize: Int
    val rootNodes: List<ITreeNode<T>>
    var treeRootInterpreter : TreeDragInterpreter<T>?

    val selectedNodeBind: IBindable<ITreeNode<T>?>
    val selectedNode : ITreeNode<T>?
    val selectedBind : IBindable<T?>
    var selected : T?

    fun addRoot( value: T, attributes: ITreeNodeAttributes<T> ) : ITreeNode<T>
    fun removeRoot( toRemove: ITreeNode<T>)
    fun clearRoots()
    fun constructTree( constructor: ITreeElementConstructor<T>.()->Unit )

    fun getNodeFromY( y: Int) : ITreeNode<T>?

    interface ITreeNode<T> {
        val children: List<ITreeNode<T>>
        var value : T
        val valueBind : Bindable<T>
        var expanded : Boolean
        val expandedBind : Bindable<Boolean>

        fun addChild(value: T, attributes: ITreeNodeAttributes<T>, expanded: Boolean = true): ITreeNode<T>
        fun removeChild( toRemove: ITreeNode<T>)
        fun clearChildren()
    }

    enum class DropDirection {ABOVE, BELOW, INTO}

    interface TreeDragInterpreter<T> {
        fun canImport( trans: ITransferObject) : Boolean
        fun interpretDrop( trans: ITransferObject, dropInto: ITreeNode<T>, dropDirection: DropDirection)
    }

    interface ITreeNodeAttributes<T> : TreeDragInterpreter<T> {
        fun makeComponent( t: T) : ITreeComponent

        fun canDrag() : Boolean = false
        fun makeCursor( t: T) : IImage = NillImage
        fun makeTransferable( t: T) : ITransferObject = StringTransferObject(toString())
        fun dragOut(t:T, up: Boolean, inArea: Boolean) {}
        override fun canImport(trans: ITransferObject) : Boolean = false
        override fun interpretDrop(trans: ITransferObject, dropInto: ITreeNode<T>, dropDirection: DropDirection) {}

        fun getBackgroundColor( t: T, isSelected: Boolean) : SColor? = null

        val isLeaf : Boolean get() = false
    }

    interface ITreeComponent {
        val component: IComponent
        val leftComponent: IComponent? get() = null
        fun onRename() {}
    }
    class SimpleTreeComponent(override val component: IComponent) : ITreeComponent


    class BasicTreeNodeAttributes<T>(val componentProvider : IComponentProvider) : ITreeNodeAttributes<T> {
        override fun makeComponent( t: T) = when( t) {
            is IComponent -> SimpleTreeComponent(t)
            else -> SimpleTreeComponent(componentProvider.Label(t.toString()))
        }
    }
}

interface ITreeView<T> : ITreeViewNonUI<T>, IComponent
{
    var backgroundColor : SColor
    var selectedColor : SColor
}

class ITreeElementConstructor<T> {
    fun Node(value: T, attributes: ITreeNodeAttributes<T>, onNodeCreated: ((ITreeNode<T>)->Unit)? = null) {
        _elements.add(ITNode(value, attributes, onNodeCreated = onNodeCreated))
    }
    fun Branch(
            value: T,
            attributes: ITreeNodeAttributes<T>,
            expanded: Boolean = true,
            onNodeCreated: ((ITreeNode<T>)->Unit)? = null,
            initializer: ITreeElementConstructor<T>.()->Unit)
    {
        _elements.add(ITNode(
                value,
                attributes,
                ITreeElementConstructor<T>().apply { initializer.invoke(this) }.elements,
                expanded,
                onNodeCreated))
    }

    internal class ITNode<T>(
            val value:T,
            val attributes: ITreeNodeAttributes<T>,
            val children: List<ITNode<T>>? = null,
            val expanded: Boolean = true,
            val onNodeCreated: ((ITreeNode<T>)->Unit)? = null)
    internal val elements : List<ITNode<T>> get() = _elements
    private val _elements  = mutableListOf<ITNode<T>>()
}

