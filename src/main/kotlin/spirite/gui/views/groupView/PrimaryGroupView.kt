package spirite.gui.views.groupView

import rb.IContract
import rb.glow.color.Colors
import rb.owl.bindable.addObserver
import rbJvm.owl.addWeakObserver
import sgui.components.IComponent
import sgui.components.IToggleButton
import sgui.components.ITreeElementConstructor
import sgui.components.ITreeView
import sgui.components.ITreeViewNonUI.*
import sgui.components.ITreeViewNonUI.DropDirection.*
import sgui.components.crossContainer.ICrossPanel
import sgui.components.events.MouseEvent
import sgui.components.events.MouseEvent.MouseButton.LEFT
import sgui.components.events.MouseEvent.MouseButton.RIGHT
import sgui.components.events.MouseEvent.MouseEventType.RELEASED
import sgui.systems.IGlobalMouseHook
import sgui.transfer.ITransferObject
import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.graphics.rendering.IThumbnailStore.IThumbnailAccessContract
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.gui.menus.NodeMenus
import spirite.gui.resources.SpiriteIcons
import spirite.gui.resources.Transferables
import sguiSwing.hybrid.Hybrid

class PrimaryGroupView
private constructor(
        private val master: IMasterControl,
        val tree : ITreeView<Node>)
    : IComponent by Hybrid.ui.ScrollContainer(tree)
{
    constructor(master: IMasterControl) : this(master, Hybrid.ui.TreeView<Node>())

    init {
        tree.leftSize = 40
    }


    val workspace get() = master.workspaceSet.currentWorkspace

    private fun rebuild() {
        val startTime = Hybrid.timing.currentMilli

        _nodeMap.clear()
        _hangingContracts.forEach { it.void() }
        _hangingContracts.clear()

        tree.buildingPaused = true
        tree.clearRoots()
        val pTree = workspace?.groupTree ?: return

        fun makeConstructor(group: GroupNode) : ITreeElementConstructor<Node>.()->Unit  {
            return {
                group.children.forEach {node ->
                    when {
                        node is GroupNode -> Branch(
                                node,
                                groupAttributes,
                                node.expanded,
                                {tree -> tree.expandedBind.addObserver { new, _ -> node.expanded = new }},
                                makeConstructor(node))
                        node is LayerNode && node.layer is SpriteLayer -> Node(node, spriteLayerAttributes)
                        else -> Node(node, nongroupAttributes)
                    }
                }
            }
        }

        val constructor = makeConstructor(pTree.root)
        tree.buildingPaused = false
        tree.constructTree (constructor)


        println("Rebuilt Tree in ${Hybrid.timing.currentMilli - startTime} ms")
    }

    // region SubComponents
    private val groupAttributes = BaseNodeAttributes()
    private val nongroupAttributes = NormalLaterNodeAttributes()
    private val spriteLayerAttributes = SpriteLayerNodeAttributes()

    private abstract inner class BaseNodeTreeComponent(val t: Node) : ITreeComponent {
        override val leftComponent: IComponent = Hybrid.ui.CrossPanel().also { comp ->
            comp.background = Colors.TRANSPARENT
            comp.opaque = false

            val visibilityButton = Hybrid.ui.ToggleButton( t.visible)
            visibilityButton.checkBind.addObserver {new, _ ->
                t.visible = new
            }
            visibilityButton.setOnIcon( SpiriteIcons.BigIcons.VisibleOn)
            _nodeMap[t] = visibilityButton

            // This hook prevents the main hook of the Group Tree from being executed
            //  (in particular, it prevents automatic selection of nodes you're toggling the visibility of)
            visibilityButton.setOffIcon( SpiriteIcons.BigIcons.VisibleOff)
            val k = Hybrid.mouseSystem.attachHook( object : IGlobalMouseHook {
                override fun processMouseEvent(evt: MouseEvent) {evt.consume()}
            }, visibilityButton)

            _hangingContracts.add(k)

            comp.setLayout {
                rows.addGap(4)
                rows += {
                    addGap(4)
                    rows.add(visibilityButton, width = 24, height = 24)
                }
                rows.addGap(4)
            }
        }
    }
    // NOTE: I hate this pattern, but it gets me out the door quicker and is technically efficient (just heavily coupled / hidden complexity)
    private val _nodeMap = mutableMapOf<Node, IToggleButton>()
    private val _hangingContracts = mutableListOf<IContract>()

    private inner class NormalNodeComponent( t: Node) : BaseNodeTreeComponent(t) {
        override val component = NodeLayerPanel(t,master)

        override fun onRename() {component.triggerRename()}
    }

    private open inner class BaseNodeAttributes : ITreeNodeAttributes<Node> {
        override fun makeComponent(t: Node) : ITreeComponent = NormalNodeComponent(t)

        override fun makeTransferable(t: Node): ITransferObject {return Transferables.NodeTransferObject(t)}

        override fun canDrag(): Boolean = true
        override fun dragOut( t: Node, up: Boolean, inArea: Boolean) {}
        override fun canImport(trans: ITransferObject) = trans.dataTypes.contains(Transferables.NodeTransferObject.Key)
        override fun interpretDrop(trans: ITransferObject, dropInto: ITreeNode<Node>, dropDirection: DropDirection) {
            val node = trans.getData(Transferables.NodeTransferObject.Key) as? Node ?: return
            val relativeTo = dropInto.value
            val groupTree = workspace?.groupTree ?: return

            if( node == relativeTo) return

            when( dropDirection) {
                ABOVE -> {groupTree.moveAbove(node, relativeTo)}
                INTO ->{groupTree.moveInto(node, relativeTo as? GroupNode ?: return)}
                BELOW -> { groupTree.moveBelow(node, relativeTo)}
            }
        }
    }
    private inner class NormalLaterNodeAttributes : BaseNodeAttributes() {
        override val isLeaf get() = true
    }

    private inner class SpriteLayerNodeAttributes: BaseNodeAttributes() {
        override fun makeComponent(t: Node) = TreeComponent(t)
        private inner class TreeComponent(t:Node) : BaseNodeTreeComponent(t) {
            override val component = SpriteLayerNodePanel(t, (t as LayerNode).layer as SpriteLayer, master)

            override fun onRename() {component.editableLabel.startEditing()}

        }
    }

    private inner class NodeLayerPanel
    private constructor(
            val node: Node,
            master: IMasterControl,
            private val imp: ICrossPanel) : IComponent by imp
    {
        constructor(node: Node, master: IMasterControl)
                : this(node,  master, Hybrid.ui.CrossPanel())

        private val thumbnail = Hybrid.ui.ImageBox()
        private val thumbnailContract : IThumbnailAccessContract?
        private val editableLabel =  Hybrid.ui.EditableLabel(node.name)

        init {
            thumbnailContract =  master.workspaceSet.currentWorkspace?.run {
                master.nativeThumbnailStore.contractThumbnail(node, this) {img ->thumbnail.setImage(img)}
            }

            imp.ref = this
            opaque = false
            background = Colors.TRANSPARENT

            //editableLabel.opaque = false
            editableLabel.textBind.addObserver { new, _ -> node.name = new }

            imp.setLayout {
                rows += {
                    add(thumbnail, 32, 32)
                    addGap(2)
                    add(editableLabel, height = 16)
                    height = 32
                }
            }
        }

        fun triggerRename() { editableLabel.startEditing()}
    }
    // endregion

    // region Bindings

    // Note: this is only an abstract binding because workspace is changing, so that which it is "bound" to is constantly
    //  changing.
    private val treeSelectionK= tree.selectedBind.addObserver { new, _ ->  workspace?.groupTree?.selectedNode = new}

    private val selectedNodeK = master.centralObservatory.selectedNode.addWeakObserver { new, _ -> tree.selected = new }

    private val groupTreeObserver = object: TreeObserver {
        override fun treeStructureChanged(evt : TreeChangeEvent) {rebuild()}
        override fun nodePropertiesChanged(node: Node, renderChanged: Boolean) {_nodeMap[node]?.checked = node.isVisible}
    }

    private var treeObsK : IContract? = null
    private val wsObsK = master.workspaceSet.workspaceObserver.addWeakObserver(
        object : WorkspaceObserver {
            override fun workspaceCreated(newWorkspace: IImageWorkspace) {}
            override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {}
            override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
                treeObsK?.void()
                rebuild()
                treeObsK = selectedWorkspace?.groupTree?.treeObservable?.addWeakObserver(groupTreeObserver)
            }
        })


    private val mouseHookK = Hybrid.mouseSystem.attachHook(object: IGlobalMouseHook {
        override fun processMouseEvent(evt: MouseEvent) {
            val point = evt.point.convert(tree)

            if( evt.button == RIGHT && evt.type == RELEASED) {
                val ws = workspace ?: return
                val node = tree.getNodeFromY(point.y)?.value
                master.contextMenus.LaunchContextMenu(evt.point, NodeMenus.schemeForNode(ws, node), node)
            }
            else if(evt.button == LEFT && evt.type == RELEASED) {
                val node = tree.getNodeFromY(point.y)?.value
                tree.selected = node ?: tree.selected
            }
        }
    }, tree)
    // endregion
}
