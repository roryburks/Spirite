package spirite.base.brains

import spirite.gui.components.major.RootWindow
import spirite.gui.components.major.work.WorkSectionView
import javax.swing.WindowConstants

interface  IFrameManager
{
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
}