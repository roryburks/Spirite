package spirite.gui.components.advanced

import CrossLayout
import rb.owl.IContract
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import rb.owl.bindable.addObserver
import rb.vectrix.mathUtil.MathUtil
import spirite.base.graphics.IImage
import spirite.base.graphics.NillImage
import spirite.base.util.delegates.OnChangeDelegate
import spirite.gui.components.advanced.ITreeElementConstructor.ITNode
import spirite.gui.components.advanced.ITreeViewNonUI.*
import spirite.gui.components.advanced.ITreeViewNonUI.DropDirection.*
import spirite.gui.components.advanced.crossContainer.CrossColInitializer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IToggleButton
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.JColor
import spirite.pc.gui.SimpleMouseListener
import spirite.pc.gui.basic.SJPanel
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.jcomponent
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.max

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

    fun addRoot( value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes()) : ITreeNode<T>
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

        fun addChild( value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes(), expanded: Boolean = true): ITreeNode<T>
        fun removeChild( toRemove: ITreeNode<T>)
        fun clearChildren()
    }

    enum class DropDirection {ABOVE, BELOW, INTO}

    interface TreeDragInterpreter<T> {
        fun canImport( trans: Transferable) : Boolean
        fun interpretDrop( trans: Transferable, dropInto: ITreeNode<T>, dropDirection: DropDirection)
    }

    interface TreeNodeAttributes<T> : TreeDragInterpreter<T> {
        fun makeComponent( t: T) : ITreeComponent = when( t) {
            is IComponent -> SimpleTreeComponent(t)
            else -> SimpleTreeComponent(Hybrid.ui.Label(t.toString()))
        }

        fun canDrag() : Boolean = false
        fun makeCursor( t: T) : IImage = NillImage
        fun makeTransferable( t: T) : Transferable = StringSelection(toString())
        fun dragOut(t:T, up: Boolean, inArea: Boolean) {}
        override fun canImport(trans: Transferable) : Boolean = false
        override fun interpretDrop(trans: Transferable, dropInto: ITreeNode<T>, dropDirection: DropDirection) {}

        fun getBackgroundColor( t: T, isSelected: Boolean) : Color? = null

        val isLeaf : Boolean get() = false
    }

    interface ITreeComponent {
        val component: IComponent
        val leftComponent: IComponent? get() = null
        fun onRename() {}
    }
    class SimpleTreeComponent(override val component: IComponent) : ITreeComponent


    class BasicTreeNodeAttributes<T> : TreeNodeAttributes<T>
}

interface ITreeView<T> : ITreeViewNonUI<T>, IComponent
{
    var backgroundColor : JColor
    var selectedColor : JColor
}

class ITreeElementConstructor<T> {
    fun Node(value: T, attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes(), onNodeCreated: ((ITreeNode<T>)->Unit)? = null) {
        _elements.add( ITNode( value, attributes,onNodeCreated = onNodeCreated))
    }
    fun Branch(
            value: T,
            attributes: TreeNodeAttributes<T> = BasicTreeNodeAttributes(),
            expanded: Boolean = true,
            onNodeCreated: ((ITreeNode<T>)->Unit)? = null,
            initializer: ITreeElementConstructor<T>.()->Unit)
    {
        _elements.add( ITNode(
                value,
                attributes,
                ITreeElementConstructor<T>().apply { initializer.invoke(this) }.elements,
                expanded,
                onNodeCreated))
    }

    internal class ITNode<T>(
            val value:T,
            val attributes: TreeNodeAttributes<T>,
            val children: List<ITNode<T>>? = null,
            val expanded: Boolean = true,
            val onNodeCreated: ((ITreeNode<T>)->Unit)? = null)
    internal val elements : List<ITNode<T>> get() = _elements
    private val _elements  = mutableListOf<ITNode<T>>()
}

class SwTreeView<T>
private constructor(private val imp : SwTreeViewImp<T>)
    : ITreeView<T>,
        IComponent by SwComponent(imp)
{
    constructor() : this(SwTreeViewImp())

    override var backgroundColor : Color by OnChangeDelegate(Skin.ContentTree.Background.jcolor) {imp.background = backgroundColor ; redraw()}
    override var selectedColor : Color by OnChangeDelegate(Skin.ContentTree.SelectedBackground.jcolor) {redraw()}

    override var gapSize by OnChangeDelegate( 12) {rebuildTree()}
    override var leftSize by OnChangeDelegate(0) {rebuildTree()}

    //fun nodeAtPoint( p: Vec2i)

    init {
        onMousePress += {
            getNodeFromY(it.point.y)?.also { selectedNode = it }
            requestFocus()
        }

        imp.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename")
        imp.actionMap.put("rename", object : AbstractAction(){
            override fun actionPerformed(e: ActionEvent?) {
                nodesAsList.firstOrNull { it == selectedNode }?.also { it.component?.onRename() }
            }
        })
    }

    override val selectedBind = Bindable<T?>(null)
    override var selected: T?
        get() = selectedBind.field
        set(value) {
            val node = nodesAsList.find { it.value == value }
            selectedBind.field = node?.value
            selectedNode = node
        }

    override val selectedNodeBind = Bindable<ITreeNode<T>?>(null)
            .also { it.addObserver { new, _ ->
                selectedBind.field = new?.value
                redraw()
            } }
    override var selectedNode: ITreeNode<T>? by selectedNodeBind

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

    private val bindKs = mutableListOf<IContract>()
    override var buildingPaused = false
    private fun rebuildTree() {
        if( buildingPaused) return
        compToNodeMap.clear()
        bindKs.forEach { it.void() }
        bindKs.clear()

        fun buildCrossForNode( node: TreeNode<T>, existingGap: Int, initializer: CrossColInitializer)
        {
            val treeComponent = node.attributes.makeComponent(node.value)
            compToNodeMap[treeComponent.component] = node
            treeComponent.leftComponent?.also { compToNodeMap[it] = node}
            node.component = treeComponent

            dnd.addDropSource(treeComponent.component.jcomponent)

            treeComponent.component.onMouseClick += {
                selectedNode = node
                this@SwTreeView.requestFocus()
            }

            initializer += {
                if( leftSize != 0) {
                    when(val lc = treeComponent.leftComponent) {
                        null -> addGap(leftSize)
                        else -> add(lc, width = leftSize)
                    }
                }
                if( existingGap != 0) addGap(existingGap)
                when {
                    node.children.any() -> {
                        val toggleButton = makeToggleButton(node.expanded)

                        toggleButton.checkBind.bindTo(node.expandedBind)
                        add(toggleButton, width = gapSize)
                    }
                    else ->addGap(gapSize)
                }
                add(treeComponent.component)
            }
            if( node.expanded)
                node.children.forEach { buildCrossForNode(it, existingGap + gapSize, initializer) }
        }

        imp.removeAll()
        imp.layout = CrossLayout.buildCrossLayout(imp) {
            _rootNodes.forEach { buildCrossForNode(it, 0, rows) }
        }
        imp.validate()
    }
    // endregion

    override fun getNodeFromY(y: Int) : TreeNode<T>? {
        if( y < 0) return null

        return compToNodeMap.entries
                .sortedBy { it.key.y }.asSequence()
                .firstOrNull { (comp, _)-> y < comp.y || y < comp.y + comp.height}
                ?.value
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
            val treeNode = context.addChild( node.value, node.attributes, node.expanded )
            node.children?.forEach { addNode(treeNode, it) }
            node.onNodeCreated?.invoke(treeNode)
        }

        for (root in roots) {
            val treeNode = TreeNode(root.value, root.attributes, root.expanded)
            _rootNodes.add(treeNode)
            root.children?.forEach { addNode(treeNode, it) }
            root.onNodeCreated?.invoke(treeNode)
        }
        rebuildTree()
    }

    override var treeRootInterpreter : TreeDragInterpreter<T>? = null

    override val rootNodes : List<TreeNode<T>> get() = _rootNodes
    private val _rootNodes = mutableListOf<TreeNode<T>>()

    private val nodesAsList : List<TreeNode<T>> get()  {
        fun getNodesFor( node: TreeNode<T>) : List<TreeNode<T>> =node.children.fold(mutableListOf()) {
            agg, it ->
            agg.add(it)
            agg.apply{addAll(getNodesFor(it))}
        }
        return _rootNodes.fold(mutableListOf()) {
            agg, it ->
            agg.add(it)
            agg.apply { addAll(getNodesFor(it))}
        }
    }

    // endregion


    private val compToNodeMap = mutableMapOf<IComponent,TreeNode<T>>()

    // region TreeNode
    inner class TreeNode<T>
    internal constructor( defaultValue: T, val attributes: TreeNodeAttributes<T>, expanded: Boolean = true)
        :ITreeNode<T>
    {
        override val expandedBind = Bindable(expanded)
                .also { it.addObserver(false) { _, _ ->
                    rebuildTree()
                } }
        override var expanded by expandedBind

        override val valueBind = Bindable(defaultValue)
                .also{it.addObserver { _, _ -> rebuildTree() }}
        override var value by valueBind
        override val children: List<TreeNode<T>> get() = _children
        private val _children = mutableListOf<TreeNode<T>>()

        internal var component: ITreeComponent? = null

        internal val y get() = MathUtil.minOrNull(component?.leftComponent?.y, component?.component?.y) ?: 0
        internal val height get() = MathUtil.minOrNull(component?.leftComponent?.height, component?.component?.height) ?: 0

        init {
            // I never love InvokeLaters.  This exists so that the batch Construct can exist without forcing this to
            //   be called before lComponent and component are built (which happens on buildTree)
            SwingUtilities.invokeLater {
                valueBind.addObserver { new, old ->
                    // Note: this prevents this Listener and thus the rebuildTree called N times when
                    if( old != new) {
                        val comp = attributes.makeComponent(new)

                        if (component != comp)
                            rebuildTree()
                        else {
                            comp.component.redraw()
                            comp.leftComponent?.redraw()
                        }
                    }
                }
            }
        }

        override fun addChild(value: T, attributes: TreeNodeAttributes<T>, expanded: Boolean) : TreeNode<T>{
            val newNode = TreeNode(value, attributes, expanded)
            _children.add(newNode)
            //rebuildTree()
            return newNode
        }

        override fun removeChild(toRemove: ITreeNode<T>) {
            _children.remove(toRemove)
            //rebuildTree()
        }
        override fun clearChildren() {
            _children.clear()
            //rebuildTree()
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

    init {
        this.onMousePress += { evt->
            dnd.dndGR.forEach { it.fire(evt.point.x, evt.point.y)}
        }
    }


    inner class  BTDnDGR(source: DragSource, comp: Component, i: Int, dls: DragGestureListener) : DragGestureRecognizer(source, comp, i, dls) {
        override fun unregisterListeners() {}

        override fun registerListeners() {
        }

        fun fire( x: Int, y: Int) {
            events = ArrayList<InputEvent>(1)
            events.add(MouseEvent(imp, 0, 0, 0, 0, 0, 0,  false))
            fireDragGestureRecognized(DnDConstants.ACTION_MOVE, Point(x,y))
        }
    }

    private inner class BTDnDManager : DropTarget(), DragSourceListener
    {

        val dragSource = DragSource.getDefaultDragSource()

        val dndGR = mutableListOf<BTDnDGR>()

        fun addDropSource(component: Component, root: Boolean = false) {
            dndGR.add(
            BTDnDGR(dragSource, component,DnDConstants.ACTION_COPY_OR_MOVE, if(root) rootDragListener else componentBasedDragListener))
            //dragSource.createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_COPY_OR_MOVE, if(root) rootDragListener else componentBasedDragListener)
        }

        override fun drop(evt: DropTargetDropEvent) {
            try {
                val draggingRelativeTo = draggingRelativeTo
                if (draggingRelativeTo == dragging && dragging != null) return

                val interpreter = (if (draggingRelativeTo == null) treeRootInterpreter else draggingRelativeTo.attributes)
                        ?: return
                if (interpreter.canImport(evt.transferable) && draggingRelativeTo != null)
                    interpreter.interpretDrop(evt.transferable, draggingRelativeTo, draggingDirection)
            }finally {
                dragging = null
            }
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
                    val n_y = node.y
                    val n_h = node.height
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

            val inArea = bounds.contains(p.x, p.y)
            val up = p.y < compToNodeMap.keys.map { it.y }.max() ?: 0
            val t = dragging?.value
            if( t != null) dragging?.attributes?.dragOut(t, up, inArea)

            dragging = null
            redraw()
        }
        // endregion

        // region DragGestureListener
        val componentBasedDragListener : DragGestureListener = object : DragGestureListener {
            override fun dragGestureRecognized(evt: DragGestureEvent) {
                if( dragging != null) return

                val node = compToNodeMap.entries.firstOrNull { it.key.jcomponent == evt.component}?.value ?: return
                recognized(evt, node)
            }
        }

        val rootDragListener = object : DragGestureListener {
            override fun dragGestureRecognized(evt: DragGestureEvent) {
                if( dragging != null) return

                val node = getNodeFromY(evt.dragOrigin.y) ?: return
                recognized(evt, node)
            }
        }

        private fun recognized( evt: DragGestureEvent, node: TreeNode<T>) {
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

    private class SwTreeViewImp<T> : SJPanel() {
        init {
            background = JColor(0,0,0,0)
            addMouseListener( SimpleMouseListener { evt ->
                context?.apply {
                    selectedNode = getNodeFromY(evt.y) ?: selectedNode
                }

                this@SwTreeViewImp.requestFocus()
            })
        }

        var context : SwTreeView<T>? = null

        override fun paint(g: Graphics) {
            g.color = context?.backgroundColor ?: Color.BLACK
            g.fillRect(0,0,width, height)
            context?.drawBg(g as Graphics2D)
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

    private fun drawBg( g2: Graphics2D) {
        compToNodeMap.forEach {
            val isSelected = selected == it.value.value
            val color = it.value.attributes.getBackgroundColor(it.value.value, isSelected) ?: when {
                isSelected -> selectedColor
                else -> null
            }
            if( color != null) {
                g2.color = color
                val h = max(it.key.height, it.value.component?.leftComponent?.height?:0)
                g2.fillRect(0, it.key.y, width, h)
            }
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
                    ABOVE -> rootNodes.firstOrNull()?.y ?: 0
                    else -> {
                        val lowest = lowestChild
                        when( lowest) {
                            null -> 0
                            else -> lowest.height
                        }
                    }
                }
                g2.drawLine(0, dy, width, dy)
            }
            dragging -> {}
            else -> {
                val comp = draggingRelativeTo
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

    init {
        dnd.addDropSource(this.jcomponent, true)
    }
}