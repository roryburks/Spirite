package spirite.gui.views.info

import rb.owl.observer
import spirite.base.brains.IMasterControl
import spirite.base.imageData.undo.IUndoEngine.UndoHistoryChangeEvent
import spirite.base.imageData.undo.UndoIndex
import spirite.gui.components.advanced.ITreeViewNonUI.SimpleTreeComponent
import spirite.gui.components.advanced.ITreeViewNonUI.TreeNodeAttributes
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid

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
    override val name: String get() = "Undo History"


    private val tree = Hybrid.ui.TreeView<UndoIndex>()

    private val _undoHistoryK = master.centralObservatory.trackingUndoHistoryObserver.addObserver(
        { evt : UndoHistoryChangeEvent ->
            val history = evt.history
            if( history != null) {
                tree.clearRoots()
                tree.constructTree {
                    history.forEach {Node(it, attributes)}
                }
            }
            tree.selected = evt.position
        }.observer()
    )

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
        _undoHistoryK.void()
        //wsobs.unbind()
    }

    private val attributes = NodeAttributes()
    private inner class NodeAttributes : TreeNodeAttributes<UndoIndex> {
        override fun makeComponent(t: UndoIndex) = SimpleTreeComponent(Hybrid.ui.Label(t.action.description))
    }
}