package spirite.pc

import spirite.base.brains.MasterControl
import spirite.base.file.LoadEngine
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.gui.components.major.RootWindow
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FATAL
import java.io.File
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

lateinit var master: MasterControl

fun main( args: Array<String>) {

    try {
        UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())
        SwingUtilities.invokeAndWait {
            EngineLaunchpoint.gle
            master = MasterControl()
            master.frameManager.initUi()

        }

        SwingUtilities.invokeLater {
            val ws1 = master.createWorkspace(640,480)
            ws1.groupTree.addNewSimpleLayer(null, "Background", FLAT, 640, 480)
            master.workspaceSet.addWorkspace(ws1)
            ws1.finishBuilding()
        }
    }catch (e : Exception) {
        e.printStackTrace()
        MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
    }
}