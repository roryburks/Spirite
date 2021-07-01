package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.imageData.layers.sprite.tools.SpriteLayerFixes
import spirite.base.exceptions.CommandNotValidException
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.StringUtil


class SpriteLayerCommandExecutor (
        val master: IMasterControl)
    :ICommandExecutor
{
    override fun executeCommand(string: String, extra: Any?): Boolean {
        val sprite: SpriteLayer
        val spritePart: SpritePart?
        when (extra) {
            is SpritePart -> {
                spritePart = extra
                sprite = extra.context
            }
            is SpriteLayer -> {
                sprite = extra
                spritePart = null
            }
            is LayerNode -> when(val layer = extra.layer) {
                is SpriteLayer -> {
                    sprite = layer
                    spritePart = null
                }
                else ->return false
            }
            else -> return false
        }

        try
        {
            commands[string]?.action?.invoke(sprite, spritePart, master)
            return true
        }
        catch (e: CommandNotValidException)
        {
            return false
        }
    }

    override val validCommands: List<String> get() = commands.values.map { it.commandString }
    override val domain: String get() = "sprite"
}

private val commands = HashMap<String,SpriteCommand>()
class SpriteCommand(
        val name: String,
        val action: (sprite: SpriteLayer, part: SpritePart?, master: IMasterControl)->Unit)
    :ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "sprite.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString) {it.workspaceSet.currentWorkspace?.groupTree?.selectedNode}
}

object SpriteCommands {
    val SplitParts = SpriteCommand("splitParts"){sprite, part, master ->
        val multiGroup = sprite.multiSelect ?: part?.run { setOf(this) } ?: throw CommandNotValidException
        val ws = sprite.workspace

        ws.undoEngine.doAsAggregateAction("Split Sprite") {
            multiGroup.forEach { sprite.removePart(it)}

            val layer = SpriteLayer(
                    ws,
                    multiGroup.map { Pair(it.handle, it.structure) },
                    sprite.type)
            ws.groupTree.importLayer(ws.groupTree.selectedNode, "Split Sprite",  layer)
        }
    }
    val SelectAll = SpriteCommand("selectAll") {sprite, part, master ->
        sprite.multiSelect = sprite.parts.toSet()
    }

    val MoveParts = SpriteCommand("moveParts") {sprite, part, master ->
        val multiGroup = sprite.multiSelect ?: part?.run { setOf(this) } ?: throw CommandNotValidException
        val toLayer = master.dialog.invokeMoveSpriteParts(multiGroup.toList()) ?: throw CommandNotValidException

        val ws = sprite.workspace
        ws.undoEngine.doAsAggregateAction("Move Sprite Parts") {
            multiGroup.forEach { sprite.removePart(it)}

            multiGroup.forEach{
                val nonduplicateName = StringUtil.getNonDuplicateName(toLayer.parts.map { it.partName }, it.partName)
                toLayer.insertPart(it.handle, it.structure.copy(partName = nonduplicateName))
            }
        }
    }

    val CopyAcrossMirrored = SpriteCommand("copyMirrored"){ sprite, part, master ->
        part ?: throw CommandNotValidException
        val ws = sprite.workspace
        val handlesToReplace = sprite.getAllLinkedLayers()
                .mapNotNull { it.parts.firstOrNull { it.partName == part?.partName } }
                .map { it.handle }
                .filter { it != part.handle }

        handlesToReplace.forEach {
            ws.mediumRepository.replaceMediumDirect( it, part.handle.medium.dupe(ws))
        }
    }

    val FlattenMaglevs = SpriteCommand("flattenMaglevs"){ sprite, part, master ->
        SpriteLayerFixes.SpriteMaglevToDynamic(sprite)
    }

    /**
     * Given a given part, goes through all sprites in its LinkedContext and if any of them are missing that part, adds it.
     */
    val FillInLinked = SpriteCommand("fill-in-linked") { sprite, part, master ->
        part ?: throw CommandNotValidException
        val ws = sprite.workspace
        ws.undoEngine.doAsAggregateAction("Fill in Linked SpritePart. Part: ${part.partName}") {
            for (spriteLayer in sprite.getAllLinkedLayers()) {
                spriteLayer.addPart(part.partName, part.depth, SpriteLayer.SpritePartAddMode.CreateIfAbsent)
            }
        }
    }

    val CopySpriteParts = SpriteCommand("copy-parts") { sprite, part, master ->
        val multiGroup = sprite.multiSelect ?: part?.run { setOf(this) } ?: throw CommandNotValidException
        val toLayer = master.dialog.invokeMoveSpriteParts(multiGroup.toList()) ?: throw CommandNotValidException
        val ws = sprite.workspace

        ws.undoEngine.doAsAggregateAction("Copy sprite Parts") {
            for (spritePart in multiGroup) {
                val newMedium = spritePart.handle.medium.dupe(ws)
                val newHandle = ws.mediumRepository.addMedium(newMedium)
                toLayer.replaceMedium(spritePart.partName, newHandle)
            }
        }
    }
}