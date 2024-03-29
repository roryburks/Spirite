package spirite.base.brains.commands

import spirite.base.brains.IFrameManager
import spirite.base.brains.IFrameManager.Views.*
import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.FrameCommandExecutor.FrameCommand.*
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug


class FrameCommandExecutor(
    val frameManager: IFrameManager,
    private val _debug : IDebug = DebugProvider.debug) : ICommandExecutor
{

    enum class FrameCommand(val string: String) : ICommand {
        UNDO_HISTORY("undoHistoryView"),
        DEBUG("debugView"),
        ANIMATION("animationView"),
        ANIMATION_STATE("animationStateView"),

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
            ANIMATION_STATE.string -> frameManager.launchView(ANIMATION_SPACE_VIEW)

            else -> _debug.handleWarning(IDebug.WarningType.REFERENCE, "Unrecognized command: frame.$string")
        }

        return true
    }

}