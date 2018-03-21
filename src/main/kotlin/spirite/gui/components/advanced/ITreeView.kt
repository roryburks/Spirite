package spirite.gui.components.advanced

import spirite.base.graphics.IImage
import spirite.base.graphics.NillImage
import spirite.base.util.delegates.OnChangeDelegate
import spirite.gui.Bindable
import spirite.gui.components.advanced.ITreeElementConstructor.ITNode
import spirite.gui.components.advanced.ITreeView.*
import spirite.gui.components.advanced.ITreeView.DropDirection.*
import spirite.gui.components.advanced.SwTreeView.TreeNode
import spirite.gui.components.advanced.crossContainer.CrossColInitializer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IToggleButton
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.jcomponent
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import javax.swing.JPanel
import javax.swing.SwingUtilities

interface ITreeView<T> : IComponent{
    var gapSize: Int
    var leftSize: Int
    val rootNodes: List<ITreeNode<T>>
    var treeRootInterpreter : TreeDragInterpreter<T>?

    fun addRoot( value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes()) : ITreeNode<T>
    fun removeRoot( toRemove: ITreeNode<T>)
    fun clearRoots()
    fun constructTree( constructor: ITreeElementConstructor<T>.()->Unit )

    interface ITreeNode<T> {
        val children: List<ITreeNode<T>>
        var value : T
        val valueBind : Bindable<T>
        var expanded : Boolean
        val expandedBind : Bindable<Boolean>

        fun addChild( value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes()): ITreeNode<T>
        fun removeChild( toRemove: ITreeNode<T>)
        fun clearChildren()
    }

    enum class DropDirection {ABOVE, BELOW, INTO}

    interface TreeDragInterpreter<T> {
        fun canImport( trans: Transferable) : Boolean
        fun interpretDrop( trans: Transferable, dropInto: ITreeNode<T>, dropDirection: DropDirection)
    }

    interface TreeNodeAttributes<T> : TreeDragInterpreter<T> {
        fun makeComponent( t: T) : IComponent = when( t) {
            is IComponent -> t
            else -> Hybrid.ui.Label(t.toString())
        }

        fun makeLeftComponent( t: T) : IComponent? = null
        fun canDrag() : Boolean = true
        fun makeCursor( t: T) : IImage = NillImage
        fun makeTransferable( t: T) : Transferable = StringSelection(toString())
        fun dragOut() {}
        override fun canImport(trans: Transferable) : Boolean = false
        override fun interpretDrop(trans: Transferable, dropInto: ITreeNode<T>, dropDirection: DropDirection) {}

        val isLeaf : Boolean get() = false
    }


    class BasicTreeNodeAttributes<T> : TreeNodeAttributes<T>
}

class ITreeElementConstructor<T> {
    fun Node(value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes()) {
        _elements.add( ITNode( value, attributes))
    }
    fun Branch( value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes(), initializer: ITreeElementConstructor<T>.()->Unit) {
        _elements.add( ITNode(
                value,
                attributes,
                ITreeElementConstructor<T>().apply { initializer.invoke(this) }.elements))
    }

    internal class ITNode<T>(
            val value:T,
            val attributes: TreeNodeAttributes<T>,
            val children: List<ITNode<T>>? = null)
    internal val elements : List<ITNode<T>> get() = _elements
    private val _elements  = mutableListOf<ITNode<T>>()
}

class SwTreeView<T>
private constructor(private val imp : SwTreeViewImp<T>)
    : ITreeView<T>,
        IComponent by SwComponent(imp)
{
    constructor() : this(SwTreeViewImp())

    override var gapSize by OnChangeDelegate( 12, {rebuildTree()})
    override var leftSize by OnChangeDelegate(0, {rebuildTree()})
    //fun nodeAtPoint( p: Vec2i)

    // region Tree Construction
    private fun makeToggleButton( checked: Boolean) : IToggleButton {
        val btn = Hybrid.ui.ToggleButton(checked)
        btn.plainStyle = true
        btn.setOnIcon(SwIcons.SmallIcons.Expanded)
        btn.setOnIconOver(SwIcons.SmallIcons.ExpandedHighlighted)
        btn.setOffIcon(SwIcons.SmallIcons.Unexpanded)
        btn.setOffIconOver(SwIcons.SmallIcons.UnexpandedHighlighted)
        return btn
    }

    private fun rebuildTree() {
        compToNodeMap.clear()
        lCompToNodeMap.clear()

        fun buildCrossForNode( node: TreeNode<T>, existingGap: Int, initializer: CrossColInitializer)
        {
            val leftComponent = node.attributes.makeLeftComponent(node.value)
            val component = node.attributes.makeComponent(node.value)
            if( leftComponent!= null) lCompToNodeMap.put(leftComponent, node)
            compToNodeMap.put(component, node)
            node.component = component
            node.lComponent = leftComponent
            dnd.addDropSource(component.jcomponent)

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
                        val toggleButton = makeToggleButton(node.expanded)
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
    // endregion

    fun getNodeFromY( y: Int) : TreeNode<T>? {
        if( y < 0) return null

        val components = compToNodeMap.keys.sortedBy { it.y }
        var componentToReturn = components.firstOrNull() ?: return null
        components.forEach {
            if( y < it.y) return compToNodeMap[componentToReturn]
            componentToReturn = it
            if( y < it.y + it.height)return compToNodeMap[componentToReturn]
        }

        return null
    }

    // region Root Manipulation
    override fun addRoot( value: T, attributes: TreeNodeAttributes<T> ) = TreeNode(value, attributes)
            .apply {
                _rootNodes.add(this)
                rebuildTree()
            }

    override fun removeRoot(toRemove: ITreeNode<T>) {
        _rootNodes.remove(toRemove)
        rebuildTree()
    }

    override fun clearRoots() {
        _rootNodes.clear()
        rebuildTree()
    }

    override fun constructTree(constructor: ITreeElementConstructor<T>.() -> Unit) {
        val roots = ITreeElementConstructor<T>().apply { constructor.invoke(this) }.elements

        fun addNode( context: TreeNode<T>, node: ITNode<T>) {
            context.addChild( node.value, node.attributes )
                    .apply { node.children?.forEach { addNode(this, it) }}
        }

        roots.forEach { root ->
            TreeNode(root.value, root.attributes).apply {
                _rootNodes.add(this)
                root.children?.forEach {addNode(this, it) }
            }
        }
        rebuildTree()
    }

    override var treeRootInterpreter : TreeDragInterpreter<T>? = null

    override val rootNodes : List<TreeNode<T>> get() = _rootNodes
    private val _rootNodes = mutableListOf<TreeNode<T>>()

    // endregion


    private val lCompToNodeMap = mutableMapOf<IComponent,TreeNode<T>>()
    private val compToNodeMap = mutableMapOf<IComponent,TreeNode<T>>()

    // region TreeNode
    inner class TreeNode<T>
    internal constructor( defaultValue: T, val attributes: TreeNodeAttributes<T>)
        :ITreeNode<T>
    {
        override val expandedBind = Bindable(true, {rebuildTree()})
        override var expanded by expandedBind

        override val valueBind = Bindable(defaultValue, {rebuildTree()})
        override var value by valueBind
        override val children: List<TreeNode<T>> get() = _children
        private val _children = mutableListOf<TreeNode<T>>()

        internal var lComponent : IComponent? = null
        internal lateinit var component : IComponent

        init {
            // I never love InvokeLaters.  This exists so that the batch Construct can exist without forcing this to
            //   be called before lComponent and component are built (which happens on buildTree)
            SwingUtilities.invokeLater {
                valueBind.addListener({
                    val newLComp = attributes.makeLeftComponent(it)
                    val newComp = attributes.makeComponent(it)

                    if (newLComp != lComponent || newComp != component)
                        rebuildTree()
                    else {
                        newLComp?.redraw()
                        newComp.redraw()
                    }
                })
            }
        }

        override fun addChild(value: T, attributes: TreeNodeAttributes<T>) : TreeNode<T>{
            val newNode = TreeNode(value, attributes)
            _children.add(newNode)
            rebuildTree()
            return newNode
        }

        override fun removeChild(toRemove: ITreeNode<T>) {
            _children.remove(toRemove)
            rebuildTree()
        }
        override fun clearChildren() {
            _children.clear()
            rebuildTree()
        }
    }
    // endRegion

    // region DnD
    private val dnd = BTDnDManager()
    init {
        imp.dropTarget = dnd
    }
    private var dragging: TreeNode<T>? = null
    private var draggingRelativeTo: TreeNode<T>? = null
    private var draggingDirection : DropDirection = ABOVE


    private inner class BTDnDManager : DropTarget(), DragGestureListener, DragSourceListener
    {

        val dragSource = DragSource.getDefaultDragSource()!!

        fun addDropSource(component: Component) {
            dragSource.createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_COPY_OR_MOVE, this)
        }

        override fun drop(evt: DropTargetDropEvent) {
            if( draggingRelativeTo == dragging && dragging != null) return

            val interpreter = (if(draggingRelativeTo == null)  treeRootInterpreter else draggingRelativeTo?.attributes) ?: return
            if( interpreter.canImport(evt.transferable))
                interpreter.interpretDrop(evt.transferable, draggingRelativeTo!!, draggingDirection)
        }

        override fun dragOver(evt: DropTargetDragEvent) {
            val oldNode = draggingRelativeTo
            val oldDir = draggingDirection

            val e_y = evt.location.y
            val node =getNodeFromY(e_y)
            draggingRelativeTo = node
            draggingDirection = when {
                node == null && e_y < 0 -> ABOVE
                node == null -> BELOW
                else -> {
                    val n_y = node.component.y
                    val n_h = node.component.height
                    when {
                        !node.attributes.isLeaf &&
                                e_y > n_y + n_h/4 &&
                                e_y < n_y + (n_h*3)/4 -> INTO
                        e_y < n_y + n_h/2 -> ABOVE
                        else -> BELOW
                    }
                }
            }

            val binding = node?.attributes ?: treeRootInterpreter
            if( binding?.canImport(evt.transferable) == true)
                evt.acceptDrag( DnDConstants.ACTION_COPY)
            else
                evt.rejectDrag()


            if( oldDir != draggingDirection || oldNode != draggingRelativeTo)
                redraw()
        }

        // region DragSourceListener
        // Note: This Listener is only used for the DragDropEnd for when things are dragged out of the tree, everything
        //  else is handled by the DropTarget object
        override fun dragOver(dsde: DragSourceDragEvent?) {}
        override fun dragExit(dse: DragSourceEvent?) {}
        override fun dropActionChanged(dsde: DragSourceDragEvent?) {}
        override fun dragEnter(dsde: DragSourceDragEvent?) {}
        override fun dragDropEnd(evt: DragSourceDropEvent) {
            val p = evt.location
            SwingUtilities.convertPointFromScreen(evt.location, imp)

            if( bounds.contains(p.x, p.y))
                dragging?.attributes?.dragOut()

            dragging = null
            redraw()
        }
        // endregion

        // region DragGestureListener
        override fun dragGestureRecognized(evt: DragGestureEvent) {
            if( dragging != null) return

            val node = compToNodeMap.entries.firstOrNull { it.key.jcomponent == evt.component}?.value ?: return
            if( node.attributes.canDrag()) {
                dragging = node

                val cursor = DragSource.DefaultMoveDrop
                val cursorImage = Hybrid.imageConverter.convertOrNull<ImageBI>(node.attributes.makeCursor(node.value))?.bi
                dragSource.startDrag(
                        evt,
                        cursor,
                        cursorImage,
                        Point(10,10),
                        node.attributes.makeTransferable(node.value),
                        this)
            }
        }
        // endregion
    }
    // endregion

    // region Implementation (Drawing Mostly)
    private class SwTreeViewImp<T> : JPanel() {
        init {
            background = Skin.Global.Bg.color
        }

        var context : SwTreeView<T>? = null

        override fun paint(g: Graphics) {
            super.paint(g)

            context?.drawDrag(g as Graphics2D)
        }
    }
    init {imp.context = this}

    private val lowestChild: TreeNode<T>? get() {
        var node = rootNodes.lastOrNull() ?: return null

        while (true) {
            node = node.children.lastOrNull() ?: return node
        }
    }

    private fun drawDrag( g2: Graphics2D) {
        val dragging = dragging ?: return
        val draggingRelativeTo = draggingRelativeTo
        val draggingDirection = draggingDirection

        g2.stroke = BasicStroke(2f)
        g2.color = Color.BLACK

        when( draggingRelativeTo) {
            null -> {
                val dy = when( draggingDirection) {
                    ABOVE -> rootNodes.firstOrNull()?.component?.y ?: 0
                    else -> {
                        val lowest = lowestChild
                        when( lowest) {
                            null -> 0
                            else -> lowest.component.y + lowest.component.height
                        }
                    }
                }
                g2.drawLine(0, dy, width, dy)
            }
            dragging -> {}
            else -> {
                val comp = draggingRelativeTo.component
                when( draggingDirection) {
                    ABOVE -> {
                        val dy = comp.y
                        g2.drawLine(0, dy, width, dy)
                    }
                    INTO -> {
                        g2.drawRect(0, comp.y, width, comp.height)
                    }
                    BELOW -> {
                        val dy = comp.y + comp.height
                        g2.drawLine(0, dy, width, dy)
                    }
                }
            }
        }
    }

    //endregion
}