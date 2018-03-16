package spirite.base.brains.commands

import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.*

class GlobalCommandExecuter : ICommandExecuter {
    enum class GlobalCommand(val string: String) : ICommand {
        PING( "ping"),
        ;

        override val commandString: String get() = "global.$string"
    }

    override val validCommands: List<String> = GlobalCommand.values().map { it.string }
    override val domain: String get() = "global"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            PING.string -> println("PING")
            else -> return false
        }
        return true
    }

}