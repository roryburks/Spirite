package demonstration

import sgui.hybrid.MDebug
import sgui.hybrid.MDebug.ErrorType.FATAL
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

object DemoLauncher {
    fun launch(frame: JFrame, width: Int? = null, height: Int? = null) {
        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())

            SwingUtilities.invokeLater {
                frame.pack()
                if( width != null && height != null)
                    frame.setSize(width, height)
                frame.isLocationByPlatform = true
                frame.isVisible = true
                frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            }
        }catch (e : Exception) {
            MDebug.handleError(FATAL, e.message ?: "Root-level exception caught.", e)
        }
    }
}