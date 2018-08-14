package spirite.gui.components.major.groupView

import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.graphics.rendering.IThumbnailStore.IThumbnailAccessContract
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.IIsolationManager.IsolationState
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.Colors
import spirite.gui.components.advanced.ITreeElementConstructor
import spirite.gui.components.advanced.ITreeView
import spirite.gui.components.advanced.ITreeViewNonUI.*
import spirite.gui.components.advanced.ITreeViewNonUI.DropDirection.*
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BASIC
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.IImageBox
import spirite.gui.components.basic.IToggleButton
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.gui.resources.Transferables.NodeTransferable
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.jcolor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage

class PrimaryGroupView
private constructor(
        private val master: IMasterControl,
        val tree : ITreeView<Node>)
    : IComponent by Hybrid.ui.ScrollContainer(tree),
        TreeObserver
{
    constructor(master: IMasterControl) : this(master, Hybrid.ui.TreeView<Node>())

    override fun treeStructureChanged(evt : TreeChangeEvent) {rebuild()}
    override fun nodePropertiesChanged(node: Node, renderChanged: Boolean) {}

    init {
        master.centralObservatory.trackingPrimaryTreeObserver.addObserver(this)

        tree.leftSize = 40

        tree.onMouseClick = { evt ->
            if( evt.button == RIGHT )
                workspace?.apply {
                    val node = tree.getNodeFromY(evt.point.y)?.value
                    master.contextMenus.LaunchContextMenu(evt.point, master.contextMenus.schemeForNode(this, node), node)
                }
        }

        // Note: this is only an abstract binding because workspace is changing, so that which it is "bound" to is constantly
        //  changing.
        tree.selectedBind.addListener { new, old ->  workspace?.groupTree?.selectedNode = new}
        master.centralObservatory.selectedNode.addListener { new, old -> tree.selected = new }
    }

    val workspace get() = master.workspaceSet.currentWorkspace

    private fun rebuild() {
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
                                {tree -> tree.expandedBind.addListener { new, _ -> node.expanded = new }},
                                makeConstructor(node))
                        node is LayerNode && node.layer is SpriteLayer -> Node(node, spriteLayerAttributes)
                        else -> Node(node, nongroupAttributes)
                    }
                }
            }
        }

        tree.constructTree (makeConstructor(pTree.root))
    }

    private val groupAttributes = BaseNodeAttributes()
    private val nongroupAttributes = NormalLaterNodeAttributes()
    private val spriteLayerAttributes = SpriteLayerNodeAttributes()

    private open inner class BaseNodeAttributes : TreeNodeAttributes<Node> {
        override fun makeLeftComponent(t: Node) : IComponent{
            val comp = Hybrid.ui.CrossPanel()
            comp.background = Colors.TRANSPARENT
            comp.opaque = false

            val visibilityButton = Hybrid.ui.ToggleButton( t.visible)
            visibilityButton.checkBind.addListener { new, old ->  t.visible = new}
            visibilityButton.setOnIcon( SwIcons.BigIcons.VisibleOn)
            visibilityButton.setOffIcon( SwIcons.BigIcons.VisibleOff)

            comp.setLayout {
                rows.addGap(4)
                rows += {
                    addGap(4)
                    rows.add(visibilityButton, width = 24, height = 24)
                }
                rows.addGap(4)
            }
            return comp
        }

        override fun makeComponent(t: Node): IComponent  {
            val comp = NodeLayerPanel(t,master)
            comp.onMouseRelease = { evt ->
                if( evt.button == RIGHT )
                    workspace?.apply {
                        master.contextMenus.LaunchContextMenu(evt.point, master.contextMenus.schemeForNode(this, t), t)
                    }
            }
            return comp
        }

        override fun makeTransferable(t: Node): Transferable {return NodeTransferable(t)}

        override fun canDrag(): Boolean = true
        override fun dragOut( t: Node, up: Boolean, inArea: Boolean) {
            val groupTree = workspace?.groupTree ?: return
            groupTree.moveInto(t, groupTree.root, up)
        }
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
        override fun makeComponent(t: Node): IComponent {
            val comp = SpriteLayerNodePanel(t, (t as LayerNode).layer as SpriteLayer, master)
            comp.onMouseRelease = { evt ->
                if( evt.button == RIGHT )
                    workspace?.apply {
                        master.contextMenus.LaunchContextMenu(evt.point, master.contextMenus.schemeForNode(this, t), t)
                    }
            }
            return comp
        }
    }

    class NodeLayerPanel
    private constructor(
            val node: Node,
            master: IMasterControl,
            private val imp: ICrossPanel) : IComponent by imp
    {
        constructor(node: Node, master: IMasterControl)
                : this(node,  master, Hybrid.ui.CrossPanel())

        private val thumbnail = Hybrid.ui.ImageBox()
        private val thumbnailContract : IThumbnailAccessContract?

        init {
            thumbnailContract =  master.workspaceSet.currentWorkspace?.run {
                master.nativeThumbnailStore.contractThumbnail(node, this) {img ->thumbnail.setImage(img)}
            }

            imp.ref = this
            opaque = false
            background = Colors.TRANSPARENT

            val editableLabel = Hybrid.ui.EditableLabel(node.name)
            //editableLabel.opaque = false
            editableLabel.textBind.addRootListener { new, old -> node.name = new }


            imp.setLayout {
                rows += {
                    add(thumbnail, 32, 32)
                    addGap(2)
                    add(editableLabel, height = 16)
                    height = 32
                }
            }
        }
    }

    class SpriteLayerNodePanel
    private constructor(
            val node: Node,
            val sprite: SpriteLayer,
            val master: IMasterControl,
            private val imp: ICrossPanel) : IComponent by imp
    {
        constructor(node: Node, sprite: SpriteLayer, master: IMasterControl)
                : this(node, sprite, master,  Hybrid.ui.CrossPanel())

        val thumbnail = Hybrid.ui.ImageBox()
        val toggleButton = Hybrid.ui.ToggleButton(false)
        val editableLabel = Hybrid.ui.EditableLabel(node.name)
        val thumbnailContract = master.workspaceSet.currentWorkspace?.run {
            master.nativeThumbnailStore.contractThumbnail(node, this) {img ->thumbnail.setImage(img)}
        }

        init {
            imp.ref = this
            opaque = false
            background = Colors.TRANSPARENT

            toggleButton.plainStyle = true
            toggleButton.setOffIcon(SwIcons.SmallIcons.Rig_New);
            toggleButton.setOnIcon(SwIcons.SmallIcons.Rig_Remove);
            toggleButton.checkBind.addRootListener { new, old ->  setLayout()}

            //editableLabel.opaque = false
            editableLabel.textBind.addRootListener { new, old -> node.name = new }

            setLayout()

            //markAsPassThrough()
        }

        var partContracts: List<IThumbnailAccessContract>? = null

        fun setLayout()
        {
            partContracts?.forEach { it.release() }
            partContracts = null

            imp.setLayout {
                rows.addFlatGroup { add(toggleButton) }
                rows += {
                    add(thumbnail, 32, 32)
                    addGap(2)
                    add(editableLabel, height = 16)
                    height = 32
                }

                if (toggleButton.checked) {
                    rows.addFlatGroup {
                        sprite.parts.forEach {part ->
                            add(SpriteLayerDisplayButton(part, master),12,12)
                            addGap(20)
                        }
                    }
                    rows.addFlatGroup(22) {
                        sprite.parts.forEach {part ->
                            val label = Hybrid.ui.Label(part.partName)
                            label.textSize = 10
                            label.textColor = Colors.BLACK.jcolor
                            add(label,32,8)
                        }
                    }
                    rows += {
                        partContracts = sprite.parts.mapNotNull { part ->
                            val partThumb = Hybrid.ui.ImageBox()
                            partThumb.checkeredBackground = true
                            add(partThumb, 32, 32)
                            master.workspaceSet.currentWorkspace?.run {
                                master.nativeThumbnailStore.contractThumbnail(part, this) {
                                    partThumb.setImage(it)
                                }
                            }
                        }
                    }
                }
            }
        }

        class SpriteLayerDisplayButton(
                val part: SpritePart,
                val master: IMasterControl) : IToggleButton by Hybrid.ui.ToggleButton(true)
        {
            init {
                //btn.plainStyle = true
                background = Skin.AnimSchemePanel.ActiveNodeBg.scolor
                setBasicBorder(BASIC)
                setOnIcon(SwIcons.SmallIcons.Rig_VisibileOn)
                setOffIcon(SwIcons.SmallIcons.Rig_VisibleOff)
                onMouseClick = {evt->
                    if( evt.button == RIGHT) {
                        val res = master.dialog.invokeDisplayOptions("Display for all Parts of kind [${part.partName}] in group.")
                        if( res != null) {
                            val workspace = part.context.workspace
                            val layer = part.context
                            val node = workspace.groupTree.root.getAllNodesSuchThat({(it as? LayerNode)?.layer == layer}).firstOrNull()
                            val parent = node?.parent
                            if( parent != null) {
                                part.context.workspace.isolationManager.setIsolationStateForSpritePartKind(parent, part.partName, true, IsolationState(res.isVisible,res.alpha))
                            }
                        }
                    }
                }
            }
        }
    }

    private val wsl = object: WorkspaceObserver {
        override fun workspaceCreated(newWorkspace: IImageWorkspace) {}
        override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {}
        override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
            rebuild()
        }
    }.apply { master.workspaceSet.workspaceObserver.addObserver(this) }
}
