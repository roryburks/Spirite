package spirite.base.brains.commands

import rb.extendo.extensions.toHashMap
import spirite.base.brains.KeyCommand
import spirite.base.exceptions.CommandNotValidException

abstract class AbstractCommandExecutor<DataSet>(
         override val domain : String,
         commmands: Collection<CommandStub<DataSet>>)
     : ICommandExecutor
{
    abstract fun makeSet(command: String, extra: Any?) : DataSet

    private val _commands = commmands
            .toHashMap { it.name }

    override val validCommands: List<String> get() = _commands.keys.toList()

    override fun executeCommand(string: String, extra: Any?): Boolean {
        try
        {
            _commands[string]?.action?.invoke(makeSet(string, extra)) ?: return false
            return true
        }catch (e : CommandNotValidException)
        {
            return false
        }
    }

    inner class WrappedStub<DataSet>(private val _stub: CommandStub<DataSet>) : ICommand {
        override val commandString: String get() = "$domain.${_stub.name}"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }
}

class CommandStub<DataSet>(
    val name: String,
    val action: (DataSet)->Unit
)
