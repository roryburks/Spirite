package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.IsolationCommandExecuter.IsolationCommand.*
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.hybrid.MDebug

class IsolationCommandExecuter(private val workspaceSet: IWorkspaceSet) : ICommandExecuter
{

    enum class IsolationCommand(val string: String) : ICommand {
        TOGGLE_ISOLATION("toggleIsolation"),
        ISOLATE_LAYER("isolateLayer"),
        CLEAR_ALL_ISOLATION("clearIsolation"),
        ;

        override val commandString: String get() = "isolation.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }

    override val validCommands: List<String> get() = IsolationCommand.values().map {  it.string }
    override val domain: String get() = "isolation"
    private val currentWorkspace get() = workspaceSet.currentWorkspace

    override fun executeCommand(string: String, extra: Any?) : Boolean{
        val workspace = currentWorkspace ?: return false
        val isolationManager = workspace.isolationManager

        when(string) {
            TOGGLE_ISOLATION.string -> isolationManager.isolationIsActive = !isolationManager.isolationIsActive
            ISOLATE_LAYER.string -> isolationManager.isolateCurrentNode = !isolationManager.isolateCurrentNode
            CLEAR_ALL_ISOLATION.string -> isolationManager.clearAllIsolation()

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: draw.$string")
        }

        return true
    }
}