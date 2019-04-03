package spirite.base.brains.commands

import rb.extendo.dataStructures.SinglySet
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.brains.MWorkspaceSet
import spirite.base.exceptions.CommandNotValidException
import spirite.base.file.workspaceFromImage
import spirite.base.graphics.Composite.SRC_IN
import spirite.base.graphics.IImage
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.RenderTarget
import spirite.base.graphics.rendering.sources.LayerSource
import spirite.base.graphics.rendering.sources.getRenderSourceForNode
import spirite.base.graphics.using
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.IClearModule
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.imageData.selection.Selection
import spirite.base.util.Colors
import spirite.base.util.linear.Rect
import spirite.gui.components.dialogs.IDialog.FilePickType
import spirite.gui.components.dialogs.IDialog.FilePickType.SAVE_SIF
import spirite.hybrid.Hybrid
import spirite.hybrid.Transferables.IClipboard.ClipboardThings.Image
import spirite.hybrid.Transferables.ILayerBuilder

class GlobalCommandExecutor(
        val master: IMasterControl,
        val workspaceSet : MWorkspaceSet)
    : ICommandExecuter
{
    override fun executeCommand(string: String, extra: Any?): Boolean {
        try
        {
            commands[string]?.action?.invoke(master, workspaceSet) ?: return false
            return true
        }catch (e : CommandNotValidException)
        {
            return false
        }
    }

    override val validCommands: List<String> get() = commands.values.map { it.commandString }
    override val domain: String get() = "global"

}

private val commands  = HashMap<String,GlobalCommand>()
class GlobalCommand(
        val name: String,
        val action: (master: IMasterControl, workspaceSet: MWorkspaceSet)->Unit)
    : ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "global.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString)
}

object GlobalCommands
{
    val NewWorkspace  = GlobalCommand("newWorkspace") { master, workspaceSet ->
        val result = master.dialog.invokeNewWorkspace() ?: throw CommandNotValidException
        val newWorkspace = master.createWorkspace(result.width, result.height)
        newWorkspace.groupTree.addNewSimpleLayer(null, "Background", DYNAMIC)
        newWorkspace.finishBuilding()
        master.workspaceSet.addWorkspace(newWorkspace)
    }

    val SaveWorkspace = GlobalCommand("saveWorkspace") {master, workspaceSet ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        val wsfile = workspace.file

        when {
            wsfile == null -> {
                val file = master.dialog.pickFile(SAVE_SIF) ?: throw CommandNotValidException
                master.fileManager.saveWorkspace(workspace, file)
            }
            workspace.hasChanged -> master.fileManager.saveWorkspace(workspace, wsfile)
        }
    }

    val SaveWorkspaceAs = GlobalCommand("saveWorkspaceAs") {master, workspaceSet ->
        val workspace = workspaceSet.currentMWorkspace  ?: throw CommandNotValidException
        val file = master.dialog.pickFile(SAVE_SIF) ?: throw CommandNotValidException
        master.fileManager.saveWorkspace(workspace, file)
    }
    val Open = GlobalCommand("open") {master, _ ->
        master.fileManager.openFile(master.dialog.pickFile(FilePickType.OPEN) ?: throw CommandNotValidException)}
    val Export = GlobalCommand("export") {master, workspaceSet ->
        master.fileManager.exportToImage(
                master.workspaceSet.currentWorkspace ?: throw CommandNotValidException,
                master.dialog.pickFile(FilePickType.EXPORT) ?: throw CommandNotValidException)

    }
    val ExportAs = GlobalCommand("exportAs", Export.action)
    val Copy = GlobalCommand("copy") {master, workspaceSet ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        copy(master, workspace, false)
    }
    val CopyVisible = GlobalCommand("copyVisible") {master, workspaceSet ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        Hybrid.clipboard.postToClipboard(copyVisible(master, workspace))
    }
    val Cut = GlobalCommand("cut") {master, workspaceSet ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        copy(master, workspace, true)
    }
    val Paste = GlobalCommand("paste") {master, workspaceSet ->
        val workspace = workspaceSet.currentMWorkspace

        val thing = Hybrid.clipboard.getFromClipboard()

        when( thing) {
            is IImage -> {
                val image : IImage = thing
                if( workspace == null)
                    master.workspaceFromImage(image)
                else {
                    val selected = workspace.groupTree.selectedNode
                    when( selected) {
                        null, is GroupNode -> workspace.groupTree.addSimpleLayerFromImage(selected, "Pasted", image)
                        else -> {
                            val workview = master.frameManager.workView?.tScreenToWorkspace ?: ImmutableTransformF.Identity
                            val pt = workview.apply(Vec2f.Zero)
                            println("$pt")
                            val x = MathUtil.clip(0, pt.x.floor, workspace.width - image.width)
                            val y = MathUtil.clip(0, pt.y.floor, workspace.height - image.height)
                            workspace.selectionEngine.setSelectionWithLifted(
                                    Selection.RectangleSelection(Rect(x, y, image.width, image.height)),
                                    LiftedImageData(image))
                        }
                    }
                }
            }
            is ILayerBuilder -> {

                if(workspace == null)
                    TODO()
                else {
                    val layer : Layer = thing.buildLayer(workspace)
                    val selected = workspace.groupTree.selectedNode
                    workspace.groupTree.importLayer(selected, "ImportedLayer", layer)
                }
            }
            else -> throw CommandNotValidException
        }
    }
    val PasteAsLayer = GlobalCommand("pasteAsLayer") { master, workspaceSet ->
        val image = (Hybrid.clipboard.getFromClipboard(SinglySet(Image)) as? IImage) ?: throw CommandNotValidException
        val workspace = master.workspaceSet.currentWorkspace
        if( workspace == null)
            master.workspaceFromImage(image)
        else
            workspace.groupTree.addSimpleLayerFromImage(workspace.groupTree.selectedNode, "Pasted", image)
    }
    val PurgeUndoHistory = GlobalCommand("purgeUndoHistory") {_, workspaceSet ->
        workspaceSet.currentWorkspace?.undoEngine?.reset()
    }


    private fun copyVisible(master: IMasterControl, workspace: IImageWorkspace) : RawImage {
        val workspaceImage = master.renderEngine.renderWorkspace(workspace)

        return workspace.selectionEngine.selection?.lift(workspaceImage, null) ?: workspaceImage.deepCopy()
    }

    private fun copy(master: IMasterControl, workspace: IImageWorkspace, cut: Boolean) {

        val selectionEngine = workspace.selectionEngine

        val node = workspace.groupTree.selectedNode

        val image = when (node) {
            null -> copyVisible(master, workspace)
            else -> {
                val selection = selectionEngine.selection
                when (selection) {
                    null -> {
                        if (cut) (workspace.activeDrawer as? IClearModule)?.clear() ?: Hybrid.beep()
                        master.renderEngine.pullImage(RenderTarget(getRenderSourceForNode(node, workspace)))
                    }
                    else -> {
                        val liftedData = selectionEngine.liftedData
                        when (liftedData) {
                            null -> {
                                // TODO: Copy to multiple data Flavors: an image one based on the renderer (what
                                //  it is now) and a Spirite one based on the ILiftedData
                                val source = getRenderSourceForNode(node, workspace)
                                val x: Int
                                val y: Int
                                if (source is LayerSource) {
                                    x = -source.layer.x
                                    y = -source.layer.y
                                } else {
                                    x = 0
                                    y = 0
                                }

                                // Flushed by the end of the function
                                val img2 = using(master.renderEngine.pullImage(RenderTarget(source))) { img ->
                                    // Flushed by the end of the function
                                    // NOTE: Might cause faint edges with rotated.  Not sure how rasterizer
                                    val transform = ImmutableTransformF.Translation(-x.f, -y.f) * (selection.transform?.invert()
                                            ?: ImmutableTransformF.Identity)

                                    val img2 = Hybrid.imageCreator.createImage(selection.mask.width, selection.mask.height)
                                    val gc = img2.graphics
                                    gc.color = Colors.WHITE
                                    gc.transform = transform
                                    gc.fillRect(0, 0, img.width, img.height)
                                    gc.composite = SRC_IN
                                    gc.transform = ImmutableTransformF.Identity
                                    gc.renderImage(selection.mask, 0, 0)

                                    gc.transform = transform
                                    gc.composite = SRC_IN
                                    gc.renderImage(img, 0, 0)

                                    img2
                                }

                                if (cut) (workspace.activeDrawer as? IClearModule)?.clear() ?: Hybrid.beep()

                                img2
                            }
                            else -> {
                                // Flushed by the end of the function
                                val img = Hybrid.imageCreator.createImage(liftedData.width, liftedData.height)
                                        .also { liftedData.draw(it.graphics) }

                                if (cut)
                                    selectionEngine.clearLifted()

                                img
                            }
                        }
                    }
                }

            }
        }
    }
}