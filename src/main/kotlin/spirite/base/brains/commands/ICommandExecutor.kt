package spirite.base.brains.commands

interface ICommandExecutor {
    val validCommands: List<String>
    val domain: String
    fun executeCommand( string: String, extra: Any?) : Boolean
}