package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.commands.NodeContextCommand.NodeCommand.*
import spirite.base.imageData.groupTree.GroupTree.Node

interface ICommandExecuter {
    val validCommands: List<String>
    val domain: String
    fun executeCommand( string: String, extra: Any?) : Boolean
}

class NodeContextCommand(val workspaceSet: IWorkspaceSet)
    : ICommandExecuter
{

    enum class NodeCommand(val string: String) : ICommand {
        NEW_GROUP( "newGroup"),
        DELETE("delete")
        ;

        override val commandString: String get() = "node.$string"
    }

    override val validCommands: List<String> get() = NodeCommand.values().map {  it.string }
    override val domain: String get() = "node"
    val currentWorskapce get() = workspaceSet.currentWorkspace

    override fun executeCommand(string: String, extra: Any?) : Boolean{
        val workspace = currentWorskapce ?: return false
        val node = extra as? Node
        when(string) {
            NEW_GROUP.string -> workspace.groupTree.addGroupNode(node, "New Group")
            DELETE.string -> node?.delete()
        }

        return true
    }

}