package spirite.pc

import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FATAL
import javax.swing.SwingUtilities
import javax.swing.UIManager

private lateinit var master: MasterControl

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
            ws1.groupTree.addNewSimpleLayer(null, "Background", DYNAMIC)
            master.workspaceSet.addWorkspace(ws1)
            ws1.finishBuilding()
        }
    }catch (e : Exception) {
        e.printStackTrace()
        MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
    }
}