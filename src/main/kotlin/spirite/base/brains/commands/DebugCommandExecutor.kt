package spirite.base.brains.commands

import rb.animo.io.aaf.reader.AafReaderFactory
import rb.animo.io.aaf.util.AafUtil
import rb.animo.io.aaf.writer.AafWriterFactory
import rb.file.BufferedReadStream
import rb.vectrix.linear.Vec2f
import rb.vectrix.rectanglePacking.ModifiedSleatorAlgorithm
import rbJvm.file.JvmInputStreamFileReader
import rbJvm.file.writing.toBufferedWriteStream
import spirite.base.brains.DBGlobal
import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.exceptions.CommandNotValidException
import spirite.base.file.aaf.export.AafExportConverter
import spirite.base.imageData.IImageObservatory
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.tools.SpriteLayerFixes
import spirite.core.hybrid.DiSet_Hybrid
import spirite.sguiHybrid.Hybrid
import java.io.File
import java.io.RandomAccessFile

class DebugCommandExecutor(
        val master: IMasterControl)
    : ICommandExecutor
{
    override fun executeCommand(string: String, extra: Any?): Boolean {
        try
        {
            commands[string]?.action?.invoke(master) ?: return false
            return true
        }catch (e : CommandNotValidException)
        {
            return false
        }
    }

    override val validCommands: List<String> get() = commands.values.map { it.commandString }
    override val domain: String get() = "debug"
}

private val commands  = HashMap<String,DebugCmd>()
class DebugCmd
internal constructor(
        val name: String,
        val action: (IMasterControl)->Unit)
    : ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "debug.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString)
}

object DebugCommands
{
    val CommandHistoryToClipboard = DebugCmd("cmdHistoryToClipboard") {
        val executedCmdString = it.commandExecutor.executedCommands
                .map { "${it.command}\t${it.extra}" }
                .joinToString("\n")
        Hybrid.clipboard.postToClipboard( executedCmdString )
    }
    val Breakpoint = DebugCmd("brk"){
        println("brk")
    }

    val CycleSpriteParts = DebugCmd("cycle-sprite-parts") {master ->
        val ws = master.workspaceSet.currentMWorkspace ?: return@DebugCmd
        val selectedNode = ws.groupTree.selectedNode as? LayerNode ?: return@DebugCmd
        val groupNode = selectedNode.parent ?: return@DebugCmd
        val sprite = selectedNode.layer as? SpriteLayer ?: return@DebugCmd
        val selected = sprite.multiSelect ?: return@DebugCmd
        val partNames = selected.map { it.partName }
        SpriteLayerFixes.CycleParts(groupNode, partNames, 1, ws )
    }

    val MaglevTotalColorChange = DebugCmd("maglev-total-color-change") { master ->
        val ws = master.workspaceSet.currentMWorkspace ?: return@DebugCmd
        val selectedNode = ws.groupTree.selectedNode ?: return@DebugCmd
        val from = master.paletteManager.activeBelt.getColor(0)
        val to = master.paletteManager.activeBelt.getColor(1)
        val mode = master.toolsetManager.toolset.ColorChanger.mode
        SpriteLayerFixes.colorChangeEntireNodeContext(selectedNode, from, to, mode, ws)
    }

    val CopyTransform = DebugCmd("copy-transform") { master ->
        val tool = master.workspaceSet.currentMWorkspace?.toolset?.Reshape ?: return@DebugCmd
        Hybrid.clipboard.postToClipboard("${tool.translation.xf};${tool.translation.yf};${tool.scale.xf};${tool.scale.yf};${tool.rotation}")
    }
    val PasteTransform = DebugCmd("paste-transform") { master ->
        val tool = master.workspaceSet.currentMWorkspace?.toolset?.Reshape ?: return@DebugCmd
        val copied = Hybrid.clipboard.getFromClipboard() as? String ?: return@DebugCmd
        val split = copied.split(';')
        val tx = split.getOrNull(0)?.toFloatOrNull() ?: 0f
        val ty = split.getOrNull(1)?.toFloatOrNull() ?: 0f
        val sx = split.getOrNull(2)?.toFloatOrNull() ?: 1f
        val sy = split.getOrNull(3)?.toFloatOrNull() ?: 1f
        val rot = split.getOrNull(4)?.toFloatOrNull() ?: 0f
        tool.translation = Vec2f(tx, ty)
        tool.scale = Vec2f(sx, sy)
        tool.rotation = rot
    }

    val ChangeFilterSet = DebugCmd("change-filter-set") {master ->
        val str = master.dialog.promptForString("Inter ToFilter Chars", "") ?: return@DebugCmd
        DBGlobal.filterSet = str.toSet()

        master.workspaceSet.currentMWorkspace?.run {
            imageObservatory?.triggerRefresh(
                IImageObservatory.ImageChangeEvent(
                    emptySet(),
                    emptySet(),
                    this,
                    true ))
        }
    }

    val TestAaf = DebugCmd("test-aaf") { master ->
        val anim = master.workspaceSet.currentMWorkspace
            ?.animationManager
            ?.currentAnimation as? FixedFrameAnimation ?: return@DebugCmd

        val converter = AafExportConverter(DiSet_Hybrid.imageCreator, ModifiedSleatorAlgorithm)
        val (aaf1, img) = converter.convert2(anim)

        // Save Aaf
        val file = File("E://bucket/sif/aaf-test.aaf")
        if( file.exists())
            file.delete()
        file.createNewFile()
        val ra = RandomAccessFile(file, "rw")
        ra.use { ra ->
            val writer = ra.toBufferedWriteStream()
            val aafWriter = AafWriterFactory.makeWriter(4)
            aafWriter.write(writer, aaf1)
        }

        // Load Aaf
        val dis = file.inputStream()
        val reader = BufferedReadStream(JvmInputStreamFileReader(dis))
        val aafReader = AafReaderFactory.readVersionAndGetReader(reader)
        val aaf2 = aafReader.read(reader)

        println(AafUtil.deepCompare(aaf1, aaf2))
    }
}
