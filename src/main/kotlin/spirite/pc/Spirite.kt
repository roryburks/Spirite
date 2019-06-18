package spirite.pc

import rbJvm.vectrix.SetupVectrixForJvm
import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FATAL
import rbJvm.glow.awt.RasterHelper
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import javax.swing.UIManager

private lateinit var master: MasterControl

fun main( args: Array<String>) {
    try {
        SetupVectrixForJvm()

        UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())
        SwingUtilities.invokeAndWait {
            val bi = BufferedImage(10,10,BufferedImage.TYPE_4BYTE_ABGR)
            RasterHelper.getDataStorageFromBi(bi)

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