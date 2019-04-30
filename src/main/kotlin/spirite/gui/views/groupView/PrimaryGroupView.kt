package spirite.gui.views.groupView

import com.sun.corba.se.impl.io.InputStreamHook
import rb.jvm.owl.addWeakObserver
import rb.owl.IContract
import rb.owl.bindable.addObserver
import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.graphics.rendering.IThumbnailStore.IThumbnailAccessContract
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.util.Colors
import spirite.gui.components.advanced.ITreeElementConstructor
import spirite.gui.components.advanced.ITreeView
import spirite.gui.components.advanced.ITreeViewNonUI.*
import spirite.gui.components.advanced.ITreeViewNonUI.DropDirection.*
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.IToggleButton
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.LEFT
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.RELEASED
import spirite.gui.resources.SwIcons
import spirite.gui.resources.Transferables.NodeTransferable
import spirite.hybrid.Hybrid
import spirite.hybrid.inputSystems.IGlobalMouseHook
import java.awt.datatransfer.Transferable

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
        _nodeMap.clear()
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

        tree.buildingPaused = false
        val constructor = makeConstructor(pTree.root)
        tree.constructTree (constructor)
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
            visibilityButton.checkBind.addObserver {new, _ ->  t.visible = new}
            visibilityButton.setOnIcon( SwIcons.BigIcons.VisibleOn)
            _nodeMap[t] = visibilityButton

            // This hook prevents the main hook of the Group Tree from being executed
            //  (in particular, it prevents automatic selection of nodes you're toggling the visibility of)
            visibilityButton.setOffIcon( SwIcons.BigIcons.VisibleOff)
            Hybrid.mouseSystem.attachHook( object : IGlobalMouseHook {
                override fun processMouseEvent(evt: MouseEvent) {evt.consume()}
            }, visibilityButton)

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

    private inner class NormalNodeComponent( t: Node) : BaseNodeTreeComponent(t) {
        override val component = NodeLayerPanel(t,master)

        override fun onRename() {component.triggerRename()}
    }

    private open inner class BaseNodeAttributes : TreeNodeAttributes<Node> {
        override fun makeComponent(t: Node) : ITreeComponent = NormalNodeComponent(t)

        override fun makeTransferable(t: Node): Transferable {return NodeTransferable(t)}

        override fun canDrag(): Boolean = true
        override fun dragOut( t: Node, up: Boolean, inArea: Boolean) {}
        override fun canImport(trans: Transferable) = trans.isDataFlavorSupported(NodeTransferable.FLAVOR)
        override fun interpretDrop(trans: Transferable, dropInto: ITreeNode<Node>, dropDirection: DropDirection) {
            val node = trans.getTransferData(NodeTransferable.FLAVOR) as? Node ?: return
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
                master.contextMenus.LaunchContextMenu(evt.point, master.contextMenus.schemeForNode(ws, node), node)
            }
            else if(evt.button == LEFT && evt.type == RELEASED) {
                val node = tree.getNodeFromY(point.y)?.value
                tree.selected = node ?: tree.selected
            }
        }
    }, tree)
    // endregion
}
