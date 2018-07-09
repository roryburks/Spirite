package spirite.base.brains.commands

import spirite.base.brains.IFrameManager
import spirite.base.brains.IFrameManager.Views.*
import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.FrameCommandExecuter.FrameCommand.*
import spirite.hybrid.MDebug


class FrameCommandExecuter(val frameManager: IFrameManager) : ICommandExecuter
{

    enum class FrameCommand(val string: String) : ICommand {
        UNDO_HISTORY("undoHistoryView"),
        DEBUG("debugView"),
        ANIMATION("animationView")

        ;

        override val commandString: String get() = "frame.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }

    override val validCommands: List<String> get() = FrameCommand.values().map {  it.string }
    override val domain: String get() = "frame"

    override fun executeCommand(string: String, extra: Any?) : Boolean{
        when(string) {
            UNDO_HISTORY.string -> frameManager.launchView(UNDO_HISTORY_VIEW)
            DEBUG.string -> frameManager.launchView(DEBUG_VIEW)
            ANIMATION.string -> frameManager.launchView(ANIMATION_VIEW)

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: frame.$string")
        }

        return true
    }

}