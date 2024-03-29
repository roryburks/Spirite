package spirite.base.brains.commands

import rb.extendo.dataStructures.SinglySet
import rb.glow.Colors
import rb.glow.Composite.SRC_IN
import rb.glow.drawer
import rb.glow.img.IImage
import rb.glow.img.RawImage
import rb.glow.using
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import sgui.core.systems.IImageCreator
import spirite.base.brains.dialog.IDialog.FilePickType
import spirite.base.brains.dialog.IDialog.FilePickType.AAF
import spirite.base.brains.dialog.IDialog.FilePickType.SAVE_SIF
import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.brains.MWorkspaceSet
import spirite.base.brains.commands.specific.LayerFixes
import spirite.base.exceptions.CommandNotValidException
import spirite.base.file.aaf.AafImporter
import spirite.base.file.workspaceFromImage
import spirite.base.graphics.drawer.IImageDrawer.IClearModule
import spirite.base.graphics.rendering.RenderTarget
import spirite.base.graphics.rendering.sources.LayerSource
import spirite.base.graphics.rendering.sources.getRenderSourceForNode
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.base.imageData.mutations.importInto
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.imageData.selection.Selection
import spirite.base.util.linear.Rect
import spirite.core.hybrid.DiSet_Hybrid
import spirite.sguiHybrid.Hybrid
import spirite.sguiHybrid.transferables.IClipboard.ClipboardThings.Image
import spirite.sguiHybrid.transferables.ILayerBuilder
import spirite.sguiHybrid.transferables.INodeBuilder

class GlobalCommandExecutor(
        val master: IMasterControl,
        val workspaceSet : MWorkspaceSet)
    : ICommandExecutor
{
    override fun executeCommand(string: String, extra: Any?): Boolean {
        try
        {
            commands[string]?.action?.invoke(master, workspaceSet, extra) ?: return false
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
class GlobalCommand
internal constructor(
        val name: String,
        val action: (master: IMasterControl, workspaceSet: MWorkspaceSet, data: Any?)->Unit)
    : ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "global.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString)
}

object GlobalCommands
{
    private val _imageCreator : IImageCreator get() = DiSet_Hybrid.imageCreator

    val NewWorkspace  = GlobalCommand("newWorkspace") { master, workspaceSet, _ ->
        val result = master.dialog.invokeWorkspaceSizeDialog("New Workspace") ?: throw CommandNotValidException
        val newWorkspace = master.createWorkspace(result.width, result.height)
        newWorkspace.groupTree.addNewSimpleLayer(null, "Background", DYNAMIC)
        newWorkspace.finishBuilding()
        master.workspaceSet.addWorkspace(newWorkspace)
    }

    val SaveWorkspace = GlobalCommand("saveWorkspace") {master, workspaceSet, _ ->
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

    val SaveWorkspaceAs = GlobalCommand("saveWorkspaceAs") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace  ?: throw CommandNotValidException
        val file = master.dialog.pickFile(SAVE_SIF) ?: throw CommandNotValidException
        master.fileManager.saveWorkspace(workspace, file)
    }
    val Open = GlobalCommand("open") {master, _, _ ->
        master.fileManager.openFile(master.dialog.pickFile(FilePickType.OPEN) ?: throw CommandNotValidException)}
    val ImportAaf = GlobalCommand("importAaf") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace  ?: throw CommandNotValidException
        val file = master.dialog.pickFile(AAF) ?: throw CommandNotValidException
        AafImporter.importIntoWorkspace(file, workspace)
    }
    val Export = GlobalCommand("export") {master, workspaceSet, _ ->
        master.fileManager.exportToImage(
                master.workspaceSet.currentWorkspace ?: throw CommandNotValidException,
                master.dialog.pickFile(FilePickType.EXPORT) ?: throw CommandNotValidException)

    }
    val ExportAs = GlobalCommand("exportAs", Export.action)
    val Copy = GlobalCommand("copy") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        copy(master, workspace, false)
    }
    val CopyVisible = GlobalCommand("copyVisible") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        Hybrid.clipboard.postToClipboard(copyVisible(master, workspace))
    }
    val Cut = GlobalCommand("cut") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        copy(master, workspace, true)
    }
    val Paste = GlobalCommand("paste") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace

        when(val thing = Hybrid.clipboard.getFromClipboard()) {
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
                val wsToImport = workspace
                        ?: (master.createWorkspace(thing.width, thing.height).also { workspaceSet.addWorkspace(it) })
                wsToImport.importInto(thing)
            }
            is INodeBuilder -> {
                val wsToImport = workspace
                        ?: (master.createWorkspace(thing.width, thing.height).also { workspaceSet.addWorkspace(it) })
                thing.buildInto(wsToImport.groupTree)
            }
            else -> throw CommandNotValidException
        }
    }
    val PasteAsLayer = GlobalCommand("pasteAsLayer") { master, workspaceSet, _ ->
        val image = (Hybrid.clipboard.getFromClipboard(SinglySet(Image)) as? IImage) ?: throw CommandNotValidException
        val workspace = master.workspaceSet.currentWorkspace
        if( workspace == null)
            master.workspaceFromImage(image)
        else
            workspace.groupTree.addSimpleLayerFromImage(workspace.groupTree.selectedNode, "Pasted", image)
    }
    val PurgeUndoHistory = GlobalCommand("purgeUndoHistory") {_, workspaceSet, _ ->
        workspaceSet.currentWorkspace?.undoEngine?.reset()
    }
    val CopyAllLayer = GlobalCommand("almightyDebug") {master, workspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        val spriteLayer = ((workspace.groupTree.selectedNode as? LayerNode)?.layer as? SpriteLayer) ?: throw CommandNotValidException
        val partName = spriteLayer.activePart?.partName ?: throw CommandNotValidException
        val med = spriteLayer.activePart?.handle?.medium ?: throw CommandNotValidException

        spriteLayer
                .getAllLinkedLayers()
                .flatMap { it.parts.asSequence().filter { it.partName == partName } }
                .filter { it.handle.medium != med }
                .forEach { workspace.mediumRepository.replaceMediumDirect(it.handle, med.dupe(workspace)) }
    }

    val ImportUnused = GlobalCommand("importUnused") { master: IMasterControl, workspaceSet: MWorkspaceSet, _ ->
        val workspace = workspaceSet.currentMWorkspace ?: throw CommandNotValidException
        LayerFixes.copyUnusedMediumsIntoSprite(workspace)
    }

    val TabWorkspace = GlobalCommand("select-next-workspace") { master, workspaceSet, _ ->
        when(val ws = workspaceSet.currentWorkspace) {
            null ->  {
                workspaceSet.currentWorkspace = workspaceSet.workspaces.firstOrNull()
            }
            else -> {
                val list = workspaceSet.workspaces
                val indexOf = list.indexOf(ws)
                if( indexOf == -1){
                    workspaceSet.currentWorkspace = workspaceSet.workspaces.firstOrNull()
                }
                else {
                    val newWs = list[(indexOf +1) % list.count()]
                    workspaceSet.currentWorkspace = newWs
                }
            }
        }
    }

    val CloseWorkspace = GlobalCommand("close-worspace") {master, workspaceSet, data ->
        val ws = when(data) {
            null -> workspaceSet.currentWorkspace
            is ImageWorkspace -> data
            else -> null
        } ?: return@GlobalCommand

        workspaceSet.removeWorkspace(ws)
    }

    // region helper
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
                        if (cut) (workspace.activeDrawer as? IClearModule)?.clear() ?: DiSet_Hybrid.beep()
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

                                    val img2 = _imageCreator.createImage(selection.mask.width, selection.mask.height)
                                    val gc = img2.graphics
                                    gc.color = Colors.WHITE
                                    gc.transform = transform
                                    gc.drawer.fillRect(0.0, 0.0, img.width.d, img.height.d)
                                    gc.composite = SRC_IN
                                    gc.transform = ImmutableTransformF.Identity
                                    gc.renderImage(selection.mask, 0.0, 0.0)

                                    gc.transform = transform
                                    gc.composite = SRC_IN
                                    gc.renderImage(img, 0.0, 0.0)

                                    img2
                                }

                                if (cut) (workspace.activeDrawer as? IClearModule)?.clear() ?: DiSet_Hybrid.beep()

                                img2
                            }
                            else -> {
                                // Flushed by the end of the function
                                val img = _imageCreator.createImage(liftedData.width, liftedData.height)
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

        Hybrid.clipboard.postToClipboard(image)
    }
    // endregion
}