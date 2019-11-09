package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.exceptions.CommandNotValidException
import spirite.hybrid.Hybrid

class DebugCommandExecutor(
        val master: IMasterControl)
    : ICommandExecutor
{
    override fun executeCommand(string: String, extra: Any?): Boolean {
        try
        {
            commands[string]?.action?.invoke(master) ?: return false
            return true
        }catch (e : CommandNotValidException)
        {
            return false
        }
    }

    override val validCommands: List<String> get() = commands.values.map { it.commandString }
    override val domain: String get() = "debug"
}

private val commands  = HashMap<String,DebugCmd>()
class DebugCmd
internal constructor(
        val name: String,
        val action: (IMasterControl)->Unit)
    : ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "debug.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString)
}

object DebugCommands
{
    val CommandHistoryToClipboard = DebugCmd("cmdHistoryToClipboard") {
        val executedCmdString = it.commandExecutor.executedCommands
                .map { "${it.command}\t${it.extra}" }
                .joinToString("\n")
        Hybrid.clipboard.postToClipboard( executedCmdString )
    }
    val Breakpoint = DebugCmd("brk"){
        println("brk")
    }
}
