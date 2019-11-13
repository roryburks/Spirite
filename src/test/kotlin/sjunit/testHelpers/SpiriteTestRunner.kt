package sjunit.testHelpers

import rbJvm.vectrix.SetupVectrixForJvm
import spirite.pc.setupSwGuiStuff
import javax.swing.SwingUtilities


fun runTest(lambda: ()->Unit) {

    SetupVectrixForJvm()
    setupSwGuiStuff()

    var exception: Throwable? = null

    SwingUtilities.invokeAndWait {
        try{
            lambda()
        }catch (th: Throwable)
        {
            exception = th
        }
    }

    exception?.apply { throw this }
}