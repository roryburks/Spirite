package spirite.base.brains.commands

import rb.extendo.extensions.toHashMap
import spirite.base.brains.KeyCommand
import spirite.base.exceptions.CommandNotValidException

abstract class AbstractCommandExecutor<DataSet>(
         override val domain : String)
     : ICommandExecutor
{
    abstract fun makeSet(command: String, extra: Any?) : DataSet

    protected val _commands = hashMapOf<String,CommandStub>()

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

    inner class CommandStub(val name: String, val action: (DataSet)->Unit) : ICommand {
        init {
            _commands[name] = this
        }
        override val commandString: String get() = "$domain.${name}"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }
}
