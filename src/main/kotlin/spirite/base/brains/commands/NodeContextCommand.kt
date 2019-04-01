package spirite.base.brains.commands

import spirite.base.brains.KeyCommand
import spirite.base.brains.MWorkspaceSet
import spirite.base.brains.commands.NodeContextCommand.NodeCommand.*
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.groupTree.MovableGroupTree
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.base.imageData.mediums.MediumType.MAGLEV
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.util.StringUtil
import spirite.gui.components.dialogs.IDialog
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug

interface ICommandExecuter {
    val validCommands: List<String>
    val domain: String
    fun executeCommand( string: String, extra: Any?) : Boolean
}

class NodeContextCommand(
        val workspaceSet: MWorkspaceSet,
        val dialogs: IDialog)
    : ICommandExecuter
{

    enum class NodeCommand(val string: String) : ICommand {
        NEW_GROUP( "newGroup"),
        NEW_SIMPLE_LAYER("newSimpleLayer"),
        NEW_SPRITE_LAYER("newSpriteLayer"),
        NEW_PUPPET_LAYER("newPuppetLayer"),
        QUICK_NEW_LAYER("newLayer"),
        DUPLICATE("duplicate"),
        COPY("copy"),
        DELETE("delete"),

        ANIM_FROM_GROUP("animationFromGroup"),
        INSERT_GROUP_IN_ANIMATION("addGroupToAnim"),
        INSERT_GROUP_IN_ANIMATION_LEXICAL("addGroupToAnim_lexical"),
        GIF_FROM_FROUP("gifFromGroup"),
        MERGE_DOWN("mergeDown"),
        NEW_RIG_ANIMATION("newRigAnimation"),
        MOVE_UP("moveUp"),
        MOVE_DOWN("moveDown"),

        CONVERT_LAYER_TO_SPRITE("converSimpleLayerToSpirite")
        ;

        override val commandString: String get() = "node.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString) {it.workspaceSet.currentWorkspace?.groupTree?.selectedNode}
    }

    override val validCommands: List<String> get() = NodeCommand.values().map {  it.string }
    override val domain: String get() = "node"
    val currentWorkspace get() = workspaceSet.currentMWorkspace

    override fun executeCommand(string: String, extra: Any?) : Boolean{
        val workspace = currentWorkspace ?: return false
        val node = extra as? Node

        when(string) {
            NEW_GROUP.string -> workspace.groupTree.selectedNode = workspace.groupTree.addGroupNode( node, "New Group")
            DELETE.string -> node?.delete()
            NEW_SIMPLE_LAYER.string -> {
                dialogs.invokeNewSimpleLayer(workspace)?.apply {
                    workspace.groupTree.addNewSimpleLayer(node, name, mediumType, width, height, true)
                }
            }
            QUICK_NEW_LAYER.string -> workspace.groupTree.addNewSimpleLayer(workspace.groupTree.selectedNode, "New Layer", DYNAMIC)
            DUPLICATE.string -> node?.also { workspace.groupTree.duplicateNode(it) }
            ANIM_FROM_GROUP.string -> {
                val groupNode = node as? GroupNode ?: return false
                val name = StringUtil.getNonDuplicateName(workspace.animationManager.animations.map { it.name },"New Animation")
                val animation = FixedFrameAnimation(name, workspace, groupNode)
                workspace.animationManager.addAnimation(animation, true)
            }
            INSERT_GROUP_IN_ANIMATION.string -> {
                val animation = workspace.animationManager.currentAnimation as? FixedFrameAnimation ?: return false
                animation.addLinkedLayer( node as? GroupNode ?: return false, true)
            }
            INSERT_GROUP_IN_ANIMATION_LEXICAL.string -> {
                val animation = workspace.animationManager.currentAnimation as? FixedFrameAnimation ?: return false
                val group = node as? GroupNode ?: return false
                animation.addLexicalLayer(group)
            }
            GIF_FROM_FROUP.string -> TODO()
            MERGE_DOWN.string -> TODO()
            NEW_RIG_ANIMATION.string -> TODO()
            MOVE_UP.string -> {
                node ?: return false

                val tree : MovableGroupTree = when {
                    node.isChildOf(workspace.groupTree.root) -> workspace.groupTree
                    else -> return false
                }

                val previousNode = node.previousNode
                when(previousNode) {
                    null -> {
                        val parent = node.parent ?: return false
                        parent.parent ?: return false
                        tree.moveAbove( node, parent)
                    }
                    is GroupNode -> tree.moveInto(node, previousNode, false)
                    else -> tree.moveAbove(node, previousNode)
                }
            }
            MOVE_DOWN.string -> {
                node ?: return false

                val tree : MovableGroupTree = when {
                    node.isChildOf(workspace.groupTree.root) -> workspace.groupTree
                    else -> return false
                }

                val nextNode = node.nextNode
                when(nextNode) {
                    null -> {
                        val parent = node.parent ?: return false
                        parent.parent ?: return false
                        tree.moveBelow( node, parent)
                    }
                    is GroupNode -> tree.moveInto(node, nextNode, true)
                    else -> tree.moveBelow(node, nextNode)
                }
            }
            NEW_SPRITE_LAYER.string -> {
                if( node is LayerNode && node.layer is SpriteLayer) {
                    val structure = node.layer.parts.map { SpritePartStructure(it.depth, it.partName) }
                    val layer = SpriteLayer(structure, workspace, node.layer.type)
                    workspace.groupTree.importLayer(node, node.name, layer)
                }
                else workspace.groupTree.addNewSpriteLayer(node, "Sprite Layer", true)
            }
            NEW_PUPPET_LAYER.string -> {
                if( node is LayerNode && node.layer is SpriteLayer) {
                    val structure = node.layer.parts.map { SpritePartStructure(it.depth, it.partName) }
                    val layer = SpriteLayer(structure, workspace, MAGLEV)
                    workspace.groupTree.importLayer(node, node.name, layer)
                }
                else workspace.groupTree.addNewSpriteLayer(node, "Puppet Layer", true, MAGLEV)
            }
            COPY.string-> {
                val layerNode = node as? LayerNode ?: return false
                Hybrid.clipboard.postToClipboard(layerNode.layer)
            }
            CONVERT_LAYER_TO_SPRITE.string -> {
                if( node?.tree != workspace.groupTree) return false
                val simpleLayer = (node as? LayerNode)?.layer as? SimpleLayer ?: return false
                val medium = simpleLayer.medium
                val type = when( medium.medium) {
                    is MaglevMedium -> MAGLEV
                    is DynamicMedium -> DYNAMIC
                    else -> return false
                }

                val spriteLayer = SpriteLayer(
                        workspace,
                        listOf(Pair(medium, SpritePartStructure(0, "base"))),
                        type)
                workspace.undoEngine.doAsAggregateAction("Convert Simple Layer to Sprite Layer") {
                    workspace.groupTree.importLayer(node, node.name, spriteLayer, workspace.groupTree.selectedNode == node)
                    node.delete()
                }
            }

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: node.$string")
        }

        return true
    }

}