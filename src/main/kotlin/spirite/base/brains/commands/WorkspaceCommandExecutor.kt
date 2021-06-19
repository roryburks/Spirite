package spirite.base.brains.commands

import spirite.base.brains.KeyCommand
import spirite.base.brains.MWorkspaceSet
import spirite.base.exceptions.CommandNotValidException
import spirite.base.imageData.MImageWorkspace
import spirite.gui.menus.dialogs.IDialog
import spirite.sguiHybrid.SwHybrid

class WorkspaceCommandExecutor (
        val workspaceSet: MWorkspaceSet,
        val dialog: IDialog)
    :ICommandExecutor
{
    override val validCommands: List<String> get() = commands.values.map { it.commandString }
    override val domain: String get() = "workspace"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        try {
            val workspace = workspaceSet.currentMWorkspace ?: return false
            commands[string]?.action?.invoke(workspace, dialog) ?: return false
            return true
        }catch (e: CommandNotValidException)
        {
            return false
        }
    }

}

private val commands = HashMap<String, WorkspaceCommand>()
class WorkspaceCommand(
        val name: String,
        val action: (workspace: MImageWorkspace, dialog: IDialog)-> Unit)
    :ICommand
{
    init { commands[name] = this }

    override val commandString: String get() = "workspace.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString)
}

object WorkspaceCommands {
    val ToggleView = WorkspaceCommand("toggleView") {workspace, dialogs ->
        workspace.viewSystem.run { view = (view + 1 ) % numActiveViews }
    }
    val ResetOtherView = WorkspaceCommand("resetOtherViews") {workspace, _ ->
        workspace.viewSystem.resetOtherViews()
    }
    val CycleView = WorkspaceCommand("cycleView")  {workspace, dialog ->
        val key = SwHybrid.keypressSystem.lastAlphaNumPressed.toInt()
        if( key > '0'.toInt() && key <= '9'.toInt()){
            val num = key - '0'.toInt()
            workspace.viewSystem.numActiveViews = num
        }
    }
    val ResizeWorkspace = WorkspaceCommand("resize") {workspace, dialog ->
        val size = dialog.invokeWorkspaceSizeDialog("New Workspace") ?: return@WorkspaceCommand
        workspace.undoEngine.doAsAggregateAction("Resize Workspace") {
            workspace.width = size.width
            workspace.height = size.height
        }
    }
}