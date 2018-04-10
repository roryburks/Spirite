package spirite.gui.components.major.groupView

import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.brains.MasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.Colors
import spirite.gui.components.advanced.ITreeElementConstructor
import spirite.gui.components.advanced.ITreeView
import spirite.gui.components.advanced.ITreeViewNonUI.*
import spirite.gui.components.advanced.ITreeViewNonUI.DropDirection.*
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.resources.SwIcons
import spirite.gui.resources.Transferables.NodeTransferable
import spirite.hybrid.Hybrid
import java.awt.datatransfer.Transferable

class PrimaryGroupView
private constructor(
        private val master: IMasterControl,
        val tree : ITreeView<Node>)
    : IComponent by tree,
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
                group.children.forEach {
                    when(it) {
                        is GroupNode -> Branch(it, groupAttributes, makeConstructor(it))
                        else -> Node(it, nongroupAttributes)
                    }
                }
            }
        }

        tree.constructTree (makeConstructor(pTree.root))
    }

    private val groupAttributes = NodeAttributes()
    private val nongroupAttributes = NongroupAttributes()

    private open inner class NodeAttributes : TreeNodeAttributes<Node> {
        override fun makeLeftComponent(t: Node) : IComponent{
            val comp = Hybrid.ui.CrossPanel()
            comp.background = Colors.TRANSPARENT
            comp.opaque = false

            val visibilityButton = Hybrid.ui.ToggleButton( t.visible)
            visibilityButton.checkBindable.addListener { new, old ->  t.visible = new}
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
            val comp = Hybrid.ui.Label(t.name)
            val node = t

            comp.onMouseClick = { evt ->
                if( evt.button == RIGHT )
                    workspace?.apply {
                        master.contextMenus.LaunchContextMenu(evt.point, master.contextMenus.schemeForNode(this, node), node)
                    }
            }
            return comp
        }

        override fun makeTransferable(t: Node): Transferable {return NodeTransferable(t)}

        override fun canImport(trans: Transferable) = trans.isDataFlavorSupported(NodeTransferable.FLAVOR)
        override fun interpretDrop(trans: Transferable, dropInto: ITreeNode<Node>, dropDirection: DropDirection) {
            val node = trans.getTransferData(NodeTransferable.FLAVOR) as Node
            val relativeTo = dropInto.value
            val groupTree = workspace?.groupTree ?: return

            when( dropDirection) {
                ABOVE -> {groupTree.moveAbove(node, relativeTo)}
                INTO ->{groupTree.moveInto(node, relativeTo as? GroupNode ?: return)}
                BELOW -> {groupTree.moveBelow(node, relativeTo)}
            }
        }
    }
    private inner class NongroupAttributes : NodeAttributes() {
        override val isLeaf get() = true
    }

    private val wsl = object: WorkspaceObserver {
        override fun workspaceCreated(newWorkspace: IImageWorkspace) {}
        override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {}
        override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
            rebuild()
        }

    }.apply { master.workspaceSet.workspaceObserver.addObserver(this) }
}
