package spirite.base.brains.commands

import spirite.base.brains.KeyCommand
import spirite.base.brains.MWorkspaceSet
import spirite.base.exceptions.CommandNotValidException
import spirite.base.imageData.MImageWorkspace
import spirite.gui.components.dialogs.IDialog

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
        when(workspace.viewSystem.view) {
            0 -> workspace.viewSystem.view = 1
            else -> workspace.viewSystem.view = 0
        }
    }
    val ResetOtherView = WorkspaceCommand("resetOtherViews") {workspace, _ ->
        workspace.viewSystem.resetOtherViews()
    }
    val CycleView = WorkspaceCommand("cycleView")  {workspace, dialog ->
        workspace.viewSystem.run { view = (view + 1 ) % numActiveViews }
    }
    val ResizeWorkspace = WorkspaceCommand("resize") {workspace, dialog ->
        val size = dialog.invokeWorkspaceSizeDialog("New Workspace") ?: return@WorkspaceCommand
        workspace.undoEngine.doAsAggregateAction("Resize Workspace") {
            workspace.width = size.width
            workspace.height = size.height
        }
    }
}