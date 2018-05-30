package spirite.base.brains

import spirite.base.brains.IFrameManager.Views
import spirite.base.brains.IFrameManager.Views.DEBUG_VIEW
import spirite.base.brains.IFrameManager.Views.UNDO_HISTORY_VIEW
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.advanced.omniContainer.SwOmniDialog
import spirite.gui.components.basic.IComponent
import spirite.gui.components.major.RootWindow
import spirite.gui.components.major.info.UndoHistoryView
import spirite.gui.components.major.work.WorkSectionView
import spirite.gui.components.views.DebugView
import spirite.pc.gui.basic.jcomponent
import java.awt.Dialog.ModalityType.MODELESS
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JDialog
import javax.swing.WindowConstants

interface  IFrameManager
{
    enum class Views ( val componentConstructor: (IMasterControl)->IOmniComponent){
        DEBUG_VIEW ({DebugView()}),
        UNDO_HISTORY_VIEW({UndoHistoryView(it)})
    }

    fun launchView( view: Views)

    fun initUi()

    val workView: WorkSectionView?
}

class SwFrameManager(val master: IMasterControl): IFrameManager {
    override fun initUi() {
        root.pack()
        root.isLocationByPlatform = true
        root.isVisible = true
        root.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }

    val root by lazy {  RootWindow(master) }

    override val workView: WorkSectionView? get() = root.view

    private val _activeViewMap = mutableMapOf<Views, SwOmniDialog>()

    override fun launchView(view: Views) {
        val diag = _activeViewMap[view]

        when( diag) {
            null -> {
                val newDiag = SwOmniDialog(view.componentConstructor(master), this)
                _activeViewMap[view] = newDiag
            }
            else -> diag.toFront()
        }
    }

    internal fun closed( diag : SwOmniDialog ) {
        _activeViewMap.values.removeIf { it == diag }
    }
}