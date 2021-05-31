package spirite.base.brains.commands

import rb.global.SuccessKind
import rb.glow.gle.RenderMethod
import spirite.sguiHybrid.Hybrid
import spirite.base.brains.KeyCommand
import spirite.base.brains.MWorkspaceSet
import spirite.base.imageData.layers.sprite.tools.SpriteLayerFixes
import spirite.base.imageData.layers.sprite.tools.SpriteLayerNormalizer
import spirite.base.exceptions.CommandNotValidException
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.groupTree.MovableGroupTree
import spirite.base.imageData.groupTree.duplicateInto
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.base.imageData.mediums.MediumType.MAGLEV
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.util.StringUtil
import spirite.gui.menus.dialogs.IDialog

class NodeContextCommand(
        val workspaceSet: MWorkspaceSet,
        val dialogs: IDialog)
    : ICommandExecutor
{
    override fun executeCommand(string: String, extra: Any?) : Boolean{
        try
        {
            val workspace = workspaceSet.currentMWorkspace ?: return false
            val node = extra as? Node

            commands[string]?.action?.invoke(workspace, node, dialogs) ?: return false
            return true
        }catch (e : CommandNotValidException)
        {
            return false
        }
    }

    override val validCommands: List<String> get() = commands.values.map { it.commandString }
    override val domain: String get() = "node"
}

private val commands = HashMap<String,NodeCommand>()
class NodeCommand(
        val name: String,
        val action: (workspace: MImageWorkspace, node: Node?, dialogs: IDialog)->Unit)
    :ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "node.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString) {it.workspaceSet.currentWorkspace?.groupTree?.selectedNode}
}

object NodeCommands {
    val NewGroup = NodeCommand("newGroup") {workspace, node, dialogs ->
        workspace.groupTree.selectedNode = workspace.groupTree.addGroupNode( node, "New Group") }
    val NewSimpleLayer = NodeCommand("newSimpleLayer") {workspace, node, dialogs ->
        dialogs.invokeNewSimpleLayer(workspace)
                ?.apply {workspace.groupTree.addNewSimpleLayer(node, name, mediumType, width, height, true)}
    }
    val NewSpriteLayer = NodeCommand("newSpriteLayer") {workspace, node, _ ->
        if( node is LayerNode && node.layer is SpriteLayer) {
            val structure = node.layer.parts.map { SpritePartStructure(it.depth, it.partName) }
            val layer = SpriteLayer(structure, workspace, node.layer.type)
            workspace.groupTree.importLayer(node, node.name, layer)

            // Note: Because Parts are only "linked" after it is imported to the group tree, the Sprite Layer can't handle itself
            //   Could presumably do it in the getter, but that might not trigger
            layer.activePart = layer.parts.firstOrNull { it.partName == node.layer.activePart?.partName} ?: layer.activePart
        }
        else workspace.groupTree.addNewSpriteLayer(node, "Sprite Layer", true)
    }
    val NewPuppetLayer = NodeCommand("newPuppetLayer") {workspace, node, _ ->
        if( node is LayerNode && node.layer is SpriteLayer) {
            val structure = node.layer.parts.map { SpritePartStructure(it.depth, it.partName) }
            val layer = SpriteLayer(structure, workspace, MAGLEV)
            workspace.groupTree.importLayer(node, node.name, layer)

            // Note: Because Parts are only "linked" after it is imported to the group tree, the Sprite Layer can't handle itself
            layer.activePart = layer.parts.firstOrNull { it.partName == node.layer.activePart?.partName} ?: layer.activePart
        }
        else workspace.groupTree.addNewSpriteLayer(node, "Puppet Layer", true, MAGLEV)
    }

    val QuickNewLayer = NodeCommand("newLayer") {workspace, _, _ ->
        workspace.groupTree.addNewSimpleLayer(workspace.groupTree.selectedNode, "New Layer", DYNAMIC)}

    val Duplicate = NodeCommand("duplicate") {workspace, node, _ ->
        node?.also { workspace.groupTree.duplicateInto(it) }}
    val Copy = NodeCommand("copy") { _, node, _ ->
        when( node) {
            is LayerNode -> Hybrid.clipboard.postToClipboard(node.layer)
            is GroupNode -> Hybrid.clipboard.postToClipboard(node)
            else -> throw CommandNotValidException
        }
    }
    val Delete = NodeCommand("delete") {_, node, _ -> node?.delete()}

    val AnimFromGroup = NodeCommand("animationFromGroup") {workspace, node, dialogs ->
        val groupNode = node as? GroupNode ?: throw CommandNotValidException
        val name = StringUtil.getNonDuplicateName(workspace.animationManager.animations.map { it.name },"New Animation")
        val animation = FixedFrameAnimation(name, workspace, groupNode)
        workspace.animationManager.addAnimation(animation, true)
    }
    val InsertGroupInAnim = NodeCommand("addGroupToAnim") {workspace, node, dialogs ->
        val animation = workspace.animationManager.currentAnimation as? FixedFrameAnimation ?: throw CommandNotValidException
        val group = node as? GroupNode ?: throw CommandNotValidException
        animation.addLinkedLayer( group, true)
    }
    val InsertLexicalLayer = NodeCommand("addGroupToAnim:Lexical") {workspace, node, dialogs ->
        val animation = workspace.animationManager.currentAnimation as? FixedFrameAnimation ?: throw CommandNotValidException
        val group = node as? GroupNode ?: throw CommandNotValidException
        animation.addLexicalLayer(group)
    }
    val InsertCascadingLayer = NodeCommand("addGroupToAnim:Cascading") {workspace, node, dialogs ->
        val animation = workspace.animationManager.currentAnimation as? FixedFrameAnimation ?: throw CommandNotValidException
        val group = node as? GroupNode ?: throw CommandNotValidException
        animation.addCascadingLayer(group)
    }

    val GifFromGroup = NodeCommand("gifFromGroup") {workspace, node, dialogs ->
        TODO() }
    val MergeDown = NodeCommand("mergeDown") {workspace, node, dialogs ->
        TODO()}
    val NewRigAnimation = NodeCommand("newRigAnimation") {workspace, node, dialogs ->
        TODO()}

    val MoveUp = NodeCommand("moveUp") {workspace, node, dialogs ->
        node ?: throw CommandNotValidException

        val tree : MovableGroupTree = when {
            node.isChildOf(workspace.groupTree.root) -> workspace.groupTree
            else ->throw CommandNotValidException
        }

        when(val previousNode = node.previousNode) {
            null -> {
                val parent = node.parent ?: throw CommandNotValidException
                parent.parent ?: throw CommandNotValidException
                tree.moveAbove( node, parent)
            }
            is GroupNode -> tree.moveInto(node, previousNode, false)
            else -> tree.moveAbove(node, previousNode)
        }
    }
    val MoveDown = NodeCommand("moveDown") {workspace, node, dialogs ->
        node ?: throw CommandNotValidException

        val tree : MovableGroupTree = when {
            node.isChildOf(workspace.groupTree.root) -> workspace.groupTree
            else -> throw CommandNotValidException
        }

        when(val nextNode = node.nextNode) {
            null -> {
                val parent = node.parent ?: throw CommandNotValidException
                parent.parent ?: throw CommandNotValidException
                tree.moveBelow( node, parent)
            }
            is GroupNode -> tree.moveInto(node, nextNode, true)
            else -> tree.moveBelow(node, nextNode)
        }
    }

    val ConvertLayerToSprite = NodeCommand("convertSimpleLayerToSprite") {workspace, node, dialogs ->
        if( node?.tree != workspace.groupTree) throw CommandNotValidException
        val simpleLayer = (node as? LayerNode)?.layer as? SimpleLayer ?: throw CommandNotValidException
        val medium = simpleLayer.medium
        val type = when( medium.medium) {
            is MaglevMedium -> MAGLEV
            is DynamicMedium -> DYNAMIC
            else -> throw CommandNotValidException
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
    val ClearViewSettings = NodeCommand("clearViewSettings") {workspace, node, _ ->
        node ?: throw CommandNotValidException
        workspace.undoEngine.doAsAggregateAction("Clear View Settings") {
            node.descendants.forEach {
                it.alpha = 1f
                it.method = RenderMethod()
            }
        }
    }
    val ToggleFlatness = NodeCommand("toggleFlatness") {workspace, node, dialogs ->
        val group = node as? GroupNode ?: throw CommandNotValidException
        group.flattened = !group.flattened
    }

    //region Sprite Layer
    val DiffuseSpriteLayer = NodeCommand("diffuseSpriteLayer") {_, node, dialogs ->
        val spriteLayer = (node as? LayerNode)?.layer as? SpriteLayer ?: throw CommandNotValidException
        val diffuseFactor = dialogs.promptForString("Enter diffuse factor", "10")?.toIntOrNull() ?: throw CommandNotValidException
        if( diffuseFactor != 1) {
            spriteLayer.remapDepth(spriteLayer.parts.map { Pair(it, it.depth * diffuseFactor) }.toMap())
        }
    }
    val ShiftSpriteLayerDepth = NodeCommand("shiftSpriteLayerDepth") {_, node, dialogs ->
        val spriteLayer = (node as? LayerNode)?.layer as? SpriteLayer ?: throw CommandNotValidException
        val shiftFactor = dialogs.promptForString("Enter diffuse factor", "10")?.toIntOrNull() ?: throw CommandNotValidException
        if( shiftFactor != 0) {
            spriteLayer.remapDepth(spriteLayer.parts.map { Pair(it, it.depth + shiftFactor) }.toMap())
        }
    }
    val FlattenAllSprites = NodeCommand("flattenAllSprites"){ workspace, node, dialogs ->
        if( node is GroupNode){
            node.children
                    .filterIsInstance<LayerNode>()
                    .map { it.layer }
                    .filterIsInstance<SpriteLayer>()
                    .forEach { SpriteLayerFixes.SpriteMaglevToDynamic(it) }
        }
        if( node is LayerNode){
            if( node.layer is SpriteLayer)
                SpriteLayerFixes.SpriteMaglevToDynamic(node.layer)
        }
    }

    val NormalizeSpriteLayers = NodeCommand("spriteLayer-Normalize") { workspace, node, dialogs ->
        node ?: throw CommandNotValidException
        val response = SpriteLayerNormalizer.normalizeSpriteLayers(node, workspace, false)
        when(response.kind()) {
            SuccessKind.Failure -> Hybrid.beep()
        }
    }

    val NormalizeSpriteLayerDepths = NodeCommand("spriteLayer-Normalize-DepthOnly") { workspace, node, dialogs ->
        node ?: throw CommandNotValidException
        val response = SpriteLayerNormalizer.normalizeSpriteLayers(node, workspace,true)
        when(response.kind()) {
            SuccessKind.Failure -> Hybrid.beep()
        }
    }
    //endregion

    val ExportGroupToSMAP0 = NodeCommand("exportGroupToSmap0") { _, node, _ ->
        val groupLayer = (node as? GroupNode) ?: throw CommandNotValidException
    }
}