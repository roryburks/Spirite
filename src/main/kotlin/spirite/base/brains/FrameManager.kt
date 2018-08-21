package spirite.base.brains

import spirite.base.brains.IFrameManager.Views
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.advanced.omniContainer.SwOmniDialog
import spirite.gui.views.RootWindow
import spirite.gui.views.animation.AnimationView
import spirite.gui.views.info.UndoHistoryView
import spirite.gui.views.work.WorkSectionView
import spirite.gui.components.views.DebugView
import javax.swing.WindowConstants

interface  IFrameManager
{
    enum class Views ( val componentConstructor: (IMasterControl)->IOmniComponent){
        DEBUG_VIEW ({DebugView()}),
        UNDO_HISTORY_VIEW({UndoHistoryView(it)}),
        ANIMATION_VIEW({ AnimationView(it) })

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