package spirite.pc

import spirite.base.brains.MasterControl
import spirite.gui.components.major.RootWindow
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FATAL
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

lateinit var master: MasterControl

fun main( args: Array<String>) {

    try {
        UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())
        SwingUtilities.invokeAndWait {
            master = MasterControl()

            val root = RootWindow(master)
            root.pack()
            root.isLocationByPlatform = true
            root.isVisible = true
            root.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }

        SwingUtilities.invokeLater {
            master.workspaceSet.addWorkspace(master.createWorkspace(600,400))
        }
    }catch (e : Exception) {
        MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
    }
}