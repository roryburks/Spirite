package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.WorkViewCommandExecutor.WorkViewCommand.*
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug

class WorkViewCommandExecutor(
    val master: IMasterControl,
    private val _debug : IDebug = DebugProvider.debug) : ICommandExecutor
{
    val view get() = master.frameManager.workView


    enum class WorkViewCommand(val string: String) : ICommand {
        ZOOM_IN( "zoomIn"),
        ZOOM_OUT("zoomOut"),
        ZOOM_IN_SLOW("zoomInSlow"),
        ZOOM_OUT_SLOW("zoomOutSlow"),
        ZOOM_0("resetZoom"),
        ;

        override val commandString: String get() = "view.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }
    override val validCommands: List<String> get() = WorkViewCommand.values().map { it.string }
    override val domain: String get() = "view"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when(string) {
            ZOOM_IN.string -> view?.zoomIn()
            ZOOM_OUT.string -> view?.zoomOut()
            ZOOM_IN_SLOW.string -> view?.zoomLevel = (view?.zoomLevel ?: 0) + 1
            ZOOM_OUT_SLOW.string -> view?.zoomLevel = (view?.zoomLevel ?: 0) - 1
            ZOOM_0.string -> view?.zoomLevel = 0

            else -> _debug.handleWarning(IDebug.WarningType.REFERENCE, "Unrecognized command: view.$string")
        }
        return true
    }
}