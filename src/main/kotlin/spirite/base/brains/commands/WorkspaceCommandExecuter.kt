package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.WorkspaceCommandExecuter.ViewCommand.*

class WorkspaceCommandExecuter(val master: IMasterControl) : ICommandExecuter
{
    val view get() = master.frameManager.workView


    enum class ViewCommand(val string: String) : ICommand {
        ZOOM_IN( "zoomIn"),
        ZOOM_OUT("zoomOut"),
        ZOOM_IN_SLOW("zoomInSlow"),
        ZOOM_OUT_SLOW("zoomOutSlow"),
        ZOOM_0("resetZoom"),
        ;

        override val commandString: String get() = "view.$string"
    }
    override val validCommands: List<String> get() = ViewCommand.values().map { it.string }
    override val domain: String get() = "view"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when(string) {
            ZOOM_IN.string -> view?.zoomIn()
            ZOOM_OUT.string -> view?.zoomOut()
            ZOOM_IN_SLOW.string -> view?.zoomLevel = (view?.zoomLevel ?: 0) + 1
            ZOOM_OUT_SLOW.string -> view?.zoomLevel = (view?.zoomLevel ?: 0) - 1
            ZOOM_0.string -> view?.zoomLevel = 0
            else -> return false
        }
        return true
    }
}