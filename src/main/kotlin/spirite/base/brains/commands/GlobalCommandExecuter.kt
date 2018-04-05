package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.*
import spirite.gui.components.dialogs.IDialog.FilePickType
import spirite.gui.components.dialogs.IDialog.FilePickType.SAVE_SIF
import java.io.File

class GlobalCommandExecuter(val master: IMasterControl) : ICommandExecuter {
    enum class GlobalCommand(val string: String) : ICommand {
        PING( "ping"),
        SAVE_WORKSPACE("saveWorkspace"),
        SAVE_WORKSPACE_AS("saveWorkspaceAs"),
        OPEN("open"),
        EXPORT("export"),
        EXPORT_AS("export_as")
        ;

        override val commandString: String get() = "global.$string"
    }

    override val validCommands: List<String> = GlobalCommand.values().map { it.string }
    override val domain: String get() = "global"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            PING.string -> println("PING")
            SAVE_WORKSPACE.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return true
                val wsfile = workspace.file

                when {
                    wsfile == null -> {
                        val file = master.dialog.pickFile(SAVE_SIF) ?: return true
                        master.fileManager.saveWorkspace(workspace, file)
                    }
                    workspace.hasChanged -> master.fileManager.saveWorkspace( workspace, wsfile)
                }
            }
            SAVE_WORKSPACE_AS.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return true
                val file = master.dialog.pickFile(SAVE_SIF) ?: return true
                master.fileManager.saveWorkspace(workspace, file)
            }
            OPEN.string -> master.fileManager.openFile(master.dialog.pickFile(FilePickType.OPEN) ?: return true)
            EXPORT.string, EXPORT_AS.string -> master.fileManager.exportToImage(
                    master.workspaceSet.currentWorkspace ?: return true,
                    master.dialog.pickFile(FilePickType.EXPORT) ?: return true)
            else -> return false
        }
        return true
    }

}