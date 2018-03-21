package spirite.pc

import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
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


            val ws = master.createWorkspace(640,480)
            ws.groupTree.addNewSimpleLayer(null, "Background", FLAT, 640, 480)
            master.workspaceSet.addWorkspace(ws)
        }

        SwingUtilities.invokeLater {
        }
    }catch (e : Exception) {
        MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
    }
}