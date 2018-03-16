package spirite.pc

import spirite.base.brains.MasterControl
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.FATAL
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants


fun main( args: Array<String>) {
//    val master = MasterControl()
//
//    try {
//        UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())
//
//        SwingUtilities.invokeLater {
//            val root = RootFrame(master)
//            root.pack()
//            root.isLocationByPlatform = true
//            root.isVisible = true
//            root.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
//        }
//    }catch (e : Exception) {
//        MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
//    }
}