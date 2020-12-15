package spirite.pc

import rb.vectrix.mathUtil.f
import rbJvm.vectrix.SetupVectrixForJvm
import sguiSwing.hybrid.EngineLaunchpoint
import sguiSwing.hybrid.MDebug
import sguiSwing.hybrid.MDebug.ErrorType.FATAL
import spirite.base.brains.MasterControl
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import javax.swing.SwingUtilities
import javax.swing.UIManager


fun main( args: Array<String>) {
    println()
    Spirite().run()
}

class Spirite {
    lateinit var master: MasterControl

    var ready : Boolean  = false ; private set

    fun run() {
        try {
            SetupVectrixForJvm()
            setupSwGuiStuff()

            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())
            SwingUtilities.invokeAndWait {
                EngineLaunchpoint.gle
                master = MasterControl()

                master.frameManager.initUi()}

            SwingUtilities.invokeLater {
                val ws1 = master.createWorkspace(640,480)
                ws1.groupTree.addNewSimpleLayer(null, "Background", DYNAMIC)
                master.workspaceSet.addWorkspace(ws1)
                ws1.finishBuilding()
                ready = true
            }
        }catch (e : Exception) {
            e.printStackTrace()
            MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
        }
    }
}
