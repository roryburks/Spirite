package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.exceptions.CommandNotValidException
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart


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
}