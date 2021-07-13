package spirite.consoleApp

import rbJvm.vectrix.SetupVectrixForJvm
import spirite.base.brains.MasterControl
import spirite.pc.setupSwGuiStuff
import spirite.sguiHybrid.EngineLaunchpoint
import javax.swing.SwingUtilities

class MasterContext {
    fun init() {
        SetupVectrixForJvm()
        setupSwGuiStuff()
        SwingUtilities.invokeAndWait {
            EngineLaunchpoint.gle
        }

        master = MasterControl()
    }

    lateinit var master: MasterControl ; private set
}