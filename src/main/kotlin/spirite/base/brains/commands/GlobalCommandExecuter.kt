package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.*
import java.io.File

class GlobalCommandExecuter(val master: IMasterControl) : ICommandExecuter {
    enum class GlobalCommand(val string: String) : ICommand {
        PING( "ping"),
        SAVE_WORKSPACE("saveWorkspace")
        ;

        override val commandString: String get() = "global.$string"
    }

    override val validCommands: List<String> = GlobalCommand.values().map { it.string }
    override val domain: String get() = "global"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            PING.string -> println("PING")
            SAVE_WORKSPACE.string -> {
                master.workspaceSet.currentWorkspace?.apply {
                    master.fileManager.saveWorkspace(this, File("C:/Bucket/1.sif"))
                }

            }
            else -> return false
        }
        return true
    }

}