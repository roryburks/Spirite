package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.commands.NodeContextCommand.NodeCommand.*
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.gui.components.dialogs.IDialog

interface ICommandExecuter {
    val validCommands: List<String>
    val domain: String
    fun executeCommand( string: String, extra: Any?) : Boolean
}

class NodeContextCommand(
        val workspaceSet: IWorkspaceSet,
        val dialogs: IDialog)
    : ICommandExecuter
{

    enum class NodeCommand(val string: String) : ICommand {
        NEW_GROUP( "newGroup"),
        DELETE("delete"),
        NEW_SIMPLE_LAYER("newSimpleLayer"),
        DUPLICATE("duplicate"),
        NEW_SPRITE_LAYER("newSpriteLayer"),
        NEW_PUPPET_LAYER("newPuppetLayer"),
        ANIM_FROM_GROUP("animationFromGroup"),
        INSERT_GROUP_IN_ANIMATION("addGroupToAnim"),
        GIF_FROM_FROUP("gifFromGroup"),
        MERGE_DOWN("mergeDown"),
        NEW_RIG_ANIMATION("newRigAnimation")
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
            NEW_SIMPLE_LAYER.string -> {
                dialogs.invokeNewSimpleLayer(workspace)?.apply {
                    workspace.groupTree.addNewSimpleLayer(node, name, mediumType, width, height, true)
                }
            }
            DUPLICATE.string -> node?.also { workspace.groupTree.duplicateNode(it) }
            NEW_SPRITE_LAYER.string -> workspace.groupTree.addNewSpriteLayer(node, "sprite")
            NEW_PUPPET_LAYER.string -> TODO()
            ANIM_FROM_GROUP.string -> TODO()
            INSERT_GROUP_IN_ANIMATION.string -> TODO()
            GIF_FROM_FROUP.string -> TODO()
            MERGE_DOWN.string -> TODO()
            NEW_RIG_ANIMATION.string -> TODO()
        }

        return true
    }

}