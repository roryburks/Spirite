package spirite.gui.components.major.info

import spirite.base.brains.IMasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.undo.IUndoEngine.UndoHistoryChangeEvent
import spirite.base.imageData.undo.UndoIndex
import spirite.base.imageData.undo.UndoableAction
import spirite.gui.components.advanced.ITreeViewNonUI.TreeNodeAttributes
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import javax.swing.JLabel

class UndoHistoryView(val master: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = Hybrid.ui.CrossPanel {
        rows.addGap(3)
        rows += {
            addGap(3)
            add(tree, flex = 100f)
            addGap(3)
            flex = 100f
        }
        rows.addGap(3)
    }

    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_UndoHistory


    private val tree = Hybrid.ui.TreeView<UndoIndex>()

    private val uhobs = { evt : UndoHistoryChangeEvent ->
        val history = evt.history
        if( history != null) {
            tree.clearRoots()
            tree.constructTree {
                history.forEach {Node(it, attributes)}
            }
        }
        tree.selected = evt.position
    }.apply { master.centralObservatory.trackingUndoHistoryObserver.addObserver(this)}

//    private val wsobs = {new : IImageWorkspace?, old : IImageWorkspace?->
//        val history = new?.undoEngine?.undoHistory
//        if( history != null) {
//            tree.clearRoots()
//            tree.constructTree {
//                history.forEach { Node(it, attributes) }
//            }
//        }
//    }.run { master.workspaceSet.currentWorkspaceBind.addListener(this) }

    override fun close() {
        master.centralObservatory.trackingUndoHistoryObserver.removeObserver(uhobs)
        //wsobs.unbind()
    }

    private val attributes = NodeAttributes()
    private inner class NodeAttributes : TreeNodeAttributes<UndoIndex> {
        override fun makeComponent(t: UndoIndex): IComponent {
            return Hybrid.ui.Label(t.action.description)
        }
    }
}