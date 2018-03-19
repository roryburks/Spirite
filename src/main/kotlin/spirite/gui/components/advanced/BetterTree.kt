package spirite.gui.components.advanced

import spirite.base.util.delegates.OnChangeDelegate
import spirite.gui.Bindable
import spirite.gui.components.advanced.crossContainer.CrossColInitializer
import spirite.gui.components.basic.IComponent
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import java.awt.datatransfer.Transferable
import javax.swing.JPanel


class SwTreeView<T>
private constructor(val imp : SwTreeViewImp<T>)
    : IComponent by SwComponent(imp)
{
    constructor() : this(SwTreeViewImp())

    var gapSize by OnChangeDelegate( 12, {rebuildTree()})
    var leftSize by OnChangeDelegate(0, {rebuildTree()})
    //fun nodeAtPoint( p: Vec2i)

    fun rebuildTree() {
        compToNodeMap.clear()
        lCompToNodeMap.clear()

        fun buildCrossForNode( node: TreeNode<T>, existingGap: Int, initializer: CrossColInitializer)
        {
            val leftComponent = node.attributes.leftComponentBuilder?.invoke(node.value)
            val component = node.attributes.componentBuilder.invoke(node.value)
            if( leftComponent!= null) lCompToNodeMap.put(leftComponent, node)
            compToNodeMap.put(component, node)
            initializer += {
                if( leftSize != 0) {
                    when(leftComponent) {
                        null -> addGap(leftSize)
                        else -> add(leftComponent, width = leftSize)
                    }
                }
                if( existingGap != 0) addGap(existingGap)
                when {
                    node.children.any() -> {
                        val toggleButton = Hybrid.ui.ToggleButton(node.expanded)
                        toggleButton.checkBindable.bindWeakly(node.expandedBind)
                        add(toggleButton, width = gapSize)
                    }
                    else ->addGap(gapSize)
                }
                add(component)
            }
            if( node.expanded)
                node.children.forEach { buildCrossForNode(it, existingGap + gapSize, initializer) }
        }

        imp.removeAll()
        imp.layout = CrossLayout.buildCrossLayout(imp, {
            _rootNodes.forEach { buildCrossForNode(it, 0, rows) }
        })
        imp.validate()


    }

    fun addRoot( value: T, attributes: TreeNodeAttributes<T>) {
        _rootNodes.add(TreeNode(value, attributes))
        rebuildTree()
    }
    fun removeRoot( toRemove: TreeNode<T>) {
        _rootNodes.remove(toRemove)
        rebuildTree()
    }
    fun clear() {
        _rootNodes.clear()
        rebuildTree()
    }

    val _rootNodes = mutableListOf<TreeNode<T>>()

    private class SwTreeViewImp<T> : JPanel() {}

    val lCompToNodeMap = mutableMapOf<IComponent,TreeNode<T>>()
    val compToNodeMap = mutableMapOf<IComponent,TreeNode<T>>()

    inner class TreeNode<T>( defaultValue: T, val attributes: TreeNodeAttributes<T>)
    {
        val expandedBind = Bindable(true, {rebuildTree()})
        val expanded by expandedBind

        val valueBind = Bindable(defaultValue, {rebuildTree()})
        var value by valueBind
        val children: List<TreeNode<T>> get() = _children
        private val _children = mutableListOf<TreeNode<T>>()

        private var lComponent : IComponent? = null
        private var component : IComponent? = null

        init {
            valueBind.addListener {
                val newLComp = attributes.leftComponentBuilder?.invoke(it)
                val newComp = attributes.componentBuilder.invoke(it)

                if( newLComp != lComponent || newComp != component)
                    rebuildTree()
            }
        }

        fun addChild( value: T, attributes: TreeNodeAttributes<T>) {
            _children.add(TreeNode(value, attributes))
            rebuildTree()
        }
        fun removeChild( toRemove: TreeNode<T>) {
            _children.remove(toRemove)
            rebuildTree()
        }
        fun clearChildren() {
            _children.clear()
            rebuildTree()
        }
    }

    interface TreeNodeAttributes<T> {
        val leftComponentBuilder : ((T) -> IComponent?)?
        val componentBuilder : (T) -> IComponent
        fun canImport( trans: Transferable) : Boolean
        //fun import
    }

}