package sguiSwing.advancedComponents

import rb.IContract
import rb.extendo.delegates.OnChangeDelegate
import rb.glow.color.SColor
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.mathUtil.MathUtil
import rbJvm.glow.awt.ImageBI
import sgui.components.IComponent
import sgui.components.IToggleButton
import sgui.components.ITreeElementConstructor
import sgui.components.ITreeElementConstructor.ITNode
import sgui.components.ITreeView
import sgui.components.ITreeViewNonUI.*
import sgui.components.ITreeViewNonUI.DropDirection.*
import sgui.components.crossContainer.CrossColInitializer
import sguiSwing.*
import sguiSwing.PrimaryIcon.*
import sguiSwing.advancedComponents.CrossContainer.CrossLayout
import sguiSwing.components.SJPanel
import sguiSwing.components.SwComponent
import sguiSwing.dragAndDrop.addDragSource
import sguiSwing.dragAndDrop.setDragTarget
import sguiSwing.mouseSystem.SimpleMouseListener
import sguiSwing.skin.Skin.ContentTree.Background
import sguiSwing.skin.Skin.ContentTree.SelectedBackground
import sguiSwing.transfer.SwTransferObjectConverter
import java.awt.*
import java.awt.dnd.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.max

class SwTreeView<T>
private constructor(private val imp : SwTreeViewImp<T>,
                    private val swImp : SwComponent = SwComponent(imp))
    : ITreeView<T>,
        IComponent by swImp
{
    constructor() : this(SwTreeViewImp())

    override var backgroundColor : SColor by OnChangeDelegate(Background.scolor) { imp.background = backgroundColor.jcolor; redraw() }
    override var selectedColor : SColor by OnChangeDelegate(SelectedBackground.scolor) { redraw() }

    override var gapSize by OnChangeDelegate(12) { rebuildTree() }
    override var leftSize by OnChangeDelegate(0) { rebuildTree() }

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

    private val compToNodeMap = mutableMapOf<IComponent, TreeNode<T>>()

    init {
        onMousePress += {evt ->
            selectedNode = getNodeFromY(evt.point.y) ?: selectedNode
            requestFocus()
        }

        imp.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename")
        imp.actionMap.put("rename", object : AbstractAction(){
            override fun actionPerformed(e: ActionEvent?) {
                nodesAsList.firstOrNull { it == selectedNode }?.also { it.component?.onRename() }
            }
        })
    }

    // region Tree Construction
    private fun makeToggleButton( checked: Boolean) : IToggleButton {
        val btn = SwingComponentProvider.ToggleButton(checked)
        btn.plainStyle = true
        btn.setOnIcon(SwPrimaryIconSet.getIcon(SmallExpanded))
        btn.setOnIconOver(SwPrimaryIconSet.getIcon(SmallExpandedHighlighted))
        btn.setOffIcon(SwPrimaryIconSet.getIcon(SmallUnexpanded))
        btn.setOffIconOver(SwPrimaryIconSet.getIcon(SmallUnexpandedHighlighted))
        return btn
    }

    private val bindKs = mutableListOf<IContract>()
    override var buildingPaused = false
    private fun rebuildTree() {
        if( buildingPaused) return
        println("Intenal build event")
        compToNodeMap.clear()
        bindKs.forEach { it.void() }
        bindKs.clear()

        fun buildCrossForNode(node: TreeNode<T>, existingGap: Int, initializer: CrossColInitializer)
        {
            val treeComponent = node.attributes.makeComponent(node.value)
            compToNodeMap[treeComponent.component] = node
            treeComponent.leftComponent?.also { compToNodeMap[it] = node}
            node.component = treeComponent

            //dnd.addDropSource(treeComponent.component.jcomponent)

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
    override fun addRoot( value: T, attributes: ITreeNodeAttributes<T>) = TreeNode(value, attributes)
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
        val paused = buildingPaused
        try {
            buildingPaused = true
            val roots = ITreeElementConstructor<T>().apply { constructor.invoke(this) }.elements

            fun addNode(context: TreeNode<T>, node: ITNode<T>) {
                val treeNode = context.addChild(node.value, node.attributes, node.expanded)
                node.children?.forEach { addNode(treeNode, it) }
                node.onNodeCreated?.invoke(treeNode)
            }

            for (root in roots) {
                val treeNode = TreeNode(root.value, root.attributes, root.expanded)
                _rootNodes.add(treeNode)
                root.children?.forEach { addNode(treeNode, it) }
                root.onNodeCreated?.invoke(treeNode)
            }
        }
        finally {
            buildingPaused = paused
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


    // region TreeNode
    inner class TreeNode<T>
    internal constructor(defaultValue: T, val attributes: ITreeNodeAttributes<T>, expanded: Boolean = true)
        : ITreeNode<T>
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

        internal val y get() = MathUtil.minOrNull(component?.leftComponent?.y, component?.component?.y)
                ?: 0
        internal val height get() = MathUtil.minOrNull(component?.leftComponent?.height, component?.component?.height)
                ?: 0

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

        override fun addChild(value: T, attributes: ITreeNodeAttributes<T>, expanded: Boolean) : TreeNode<T> {
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
    // endregion

    // region DnD

    private var dragging: TreeNode<T>? = null
    private var draggingRelativeTo: TreeNode<T>? = null
    private var draggingDirection : DropDirection = ABOVE
    private val dragSource = DragSource.getDefaultDragSource()

    private val dropTargetListener = object : DropTargetListener {
        override fun dropActionChanged(dtde: DropTargetDragEvent) {}
        override fun dragExit(dte: DropTargetEvent) {}
        override fun dragEnter(dtde: DropTargetDragEvent) {}

        override fun drop(evt: DropTargetDropEvent) {
            try {
                val draggingRelativeTo = draggingRelativeTo
                if (draggingRelativeTo == dragging && dragging != null) return

                val interpreter = (draggingRelativeTo?.attributes ?: treeRootInterpreter)
                        ?: return
                if (interpreter.canImport(SwTransferObjectConverter.convert(evt.transferable)) && draggingRelativeTo != null)
                    interpreter.interpretDrop(SwTransferObjectConverter.convert(evt.transferable), draggingRelativeTo, draggingDirection)
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
            if( binding?.canImport(SwTransferObjectConverter.convert(evt.transferable)) == true)
                evt.acceptDrag(DnDConstants.ACTION_COPY)
            else
                evt.rejectDrag()


            if( oldDir != draggingDirection || oldNode != draggingRelativeTo)
                redraw()
        }
    }

    private val dragSourceListener = object : DragSourceListener {
        override fun dropActionChanged(dsde: DragSourceDragEvent?) {}
        override fun dragOver(dsde: DragSourceDragEvent?) {}
        override fun dragExit(dse: DragSourceEvent?) {}
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
    }

    private val dragGestureListener = object : DragGestureListener {
        override fun dragGestureRecognized(evt: DragGestureEvent) {
            if( dragging != null) return

            val node = getNodeFromY(evt.dragOrigin.y) ?: return
            recognized(evt, node)
        }
    }
    fun recognized(evt: DragGestureEvent, node: TreeNode<T>) {
        if (node.attributes.canDrag()) {
            dragging = node

            val cursor = DragSource.DefaultMoveDrop
            val cursorImage = SwProvider.convertOrNull<ImageBI>(node.attributes.makeCursor(node.value))?.bi
            dragSource.startDrag(
                    evt,
                    cursor,
                    cursorImage,
                    Point(10, 10),
                    SwTransferObjectConverter.convert(node.attributes.makeTransferable(node.value)),
                    dragSourceListener)
        }
    }
    init {
        swImp.setDragTarget(dropTargetListener)
        swImp.addDragSource(dragGestureListener)
    }

    // endregion

    // region Implementation (Drawing Mostly)

    private class SwTreeViewImp<T> : SJPanel() {
        init {
            background = JColor(0, 0, 0, 0)
            addMouseListener(SimpleMouseListener { evt ->
                context?.apply {
                    selectedNode = getNodeFromY(evt.y) ?: selectedNode
                }

                this@SwTreeViewImp.requestFocus()
            })
        }

        var context : SwTreeView<T>? = null

        override fun paint(g: Graphics) {
            g.color = context?.backgroundColor?.jcolor ?: Color.BLACK
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
                g2.color = color.jcolor
                val h = max(it.key.height, it.value.component?.leftComponent?.height ?: 0)
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
}