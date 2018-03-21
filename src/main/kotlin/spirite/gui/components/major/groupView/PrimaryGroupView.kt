package spirite.gui.components.major.groupView

import spirite.base.brains.IMasterControl
import spirite.base.brains.MasterControl
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.gui.SUIPoint
import spirite.gui.components.advanced.ITreeElementConstructor
import spirite.gui.components.advanced.ITreeView
import spirite.gui.components.advanced.ITreeView.*
import spirite.gui.components.advanced.ITreeView.DropDirection.*
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.resources.Transferables.NodeTransferable
import spirite.hybrid.Hybrid
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class PrimaryGroupView
private constructor(
        private val master: IMasterControl,
        val tree : ITreeView<Node>)
    : IComponent by tree,
        TreeObserver
{
    override fun treeStructureChanged() {rebuild()}
    init {
        master.centralObservatory.trackingPrimaryTreeObserver.addObserver(this)

        tree.onMouseClick = { evt ->
            if( evt.button == RIGHT )
                workspace?.apply {
                    master.contextMenus.LaunchContextMenu(evt.point, master.contextMenus.schemeForNode(this, null), null)
                }
        }
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
        override fun makeLeftComponent(t: Node): IComponent? = Hybrid.ui.Button(t.name)
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

    constructor(master: MasterControl) : this(master, Hybrid.ui.TreeView<Node>())
}
