package spirite.base.brains

import spirite.base.brains.IFrameManager.Views
import spirite.base.brains.IFrameManager.Views.DEBUG_VIEW
import spirite.base.brains.IFrameManager.Views.UNDO_HISTORY_VIEW
import spirite.gui.components.major.RootWindow
import spirite.gui.components.major.info.UndoHistoryPanel
import spirite.gui.components.major.work.WorkSectionView
import spirite.gui.components.views.UndoHistoryView
import spirite.pc.gui.basic.jcomponent
import java.awt.Dialog.ModalityType.MODELESS
import javax.swing.JDialog
import javax.swing.WindowConstants

interface  IFrameManager
{
    enum class Views {
        DEBUG_VIEW,
        UNDO_HISTORY_VIEW
    }

    fun launchView( view: Views)

    fun initUi()

    val workView: WorkSectionView?
}

class JFrameManager(val master: IMasterControl): IFrameManager {
    override fun initUi() {
        root.pack()
        root.isLocationByPlatform = true
        root.isVisible = true
        root.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }

    val root by lazy {  RootWindow(master) }

    override val workView: WorkSectionView? get() = root.view

    private val _activeViewMap = mutableMapOf<Views, JDialog>()

    override fun launchView(view: Views) {
        val diag = _activeViewMap[view]

        when( diag) {
            null -> {
                val launching = JDialog()

                when( view) {
                    DEBUG_VIEW -> {
                        launching.add(UndoHistoryView().jcomponent)
                    }
                    UNDO_HISTORY_VIEW -> {
                        launching.add(UndoHistoryPanel(master).component.jcomponent)
                    }
                }
                launching.modalityType = MODELESS
                launching.pack()
                launching.isVisible = true
                _activeViewMap[view] = launching
            }
            else -> diag.toFront()
        }

    }
}