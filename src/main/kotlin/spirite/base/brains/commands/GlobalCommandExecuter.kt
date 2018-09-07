package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.*
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
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.imageData.selection.LiftedImageData
import spirite.base.imageData.selection.Selection
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.gui.components.dialogs.IDialog.FilePickType
import spirite.gui.components.dialogs.IDialog.FilePickType.SAVE_SIF
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug

class GlobalCommandExecuter(val master: IMasterControl) : ICommandExecuter {
    enum class GlobalCommand(val string: String) : ICommand {
        NEW_WORKSPACE("newWorkspace"),
        SAVE_WORKSPACE("saveWorkspace"),
        SAVE_WORKSPACE_AS("saveWorkspaceAs"),
        OPEN("open"),
        EXPORT("export"),
        EXPORT_AS("export_as"),
        COPY("copy"),
        COPY_VISIBLE("copyVisible"),
        CUT("cut"),
        PASTE("paste"),
        PASTE_AS_LAYER("pasteAsLayer"),
        ;

        override val commandString: String get() = "global.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }

    override val validCommands: List<String> = GlobalCommand.values().map { it.string }
    override val domain: String get() = "global"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            NEW_WORKSPACE.string -> {
                val result = master.dialog.invokeNewWorkspace() ?: return false
                val newWorkspace = master.createWorkspace(result.width, result.height)
                newWorkspace.groupTree.addNewSimpleLayer(null, "Background", DYNAMIC)
                newWorkspace.finishBuilding()
                master.workspaceSet.addWorkspace(newWorkspace)
            }
            SAVE_WORKSPACE.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return false
                val wsfile = workspace.file

                when {
                    wsfile == null -> {
                        val file = master.dialog.pickFile(SAVE_SIF) ?: return true
                        master.fileManager.saveWorkspace(workspace, file)
                    }
                    workspace.hasChanged -> master.fileManager.saveWorkspace( workspace, wsfile)
                }
            }
            SAVE_WORKSPACE_AS.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return false
                val file = master.dialog.pickFile(SAVE_SIF) ?: return true
                master.fileManager.saveWorkspace(workspace, file)
            }
            OPEN.string -> master.fileManager.openFile(master.dialog.pickFile(FilePickType.OPEN) ?: return false)
            EXPORT.string,
            EXPORT_AS.string -> master.fileManager.exportToImage(
                    master.workspaceSet.currentWorkspace ?: return true,
                    master.dialog.pickFile(FilePickType.EXPORT) ?: return true)
            COPY.string -> {
                val workspace = master.workspaceSet.currentWorkspace?: return false
                copy(workspace, false)
            }
            COPY_VISIBLE.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return false
                Hybrid.imageIO.imageToClipboard(copyVisible(workspace))
            }
            CUT.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return false
                copy(workspace, true)
            }
            PASTE.string -> {
                val image = Hybrid.imageIO.imageFromClipboard() ?: return false
                val workspace = master.workspaceSet.currentWorkspace
                if( workspace == null)
                    master.workspaceFromImage(image)
                else {
                    val selected = workspace.groupTree.selectedNode
                    when( selected) {
                        null, is GroupNode -> workspace.groupTree.addSimpleLayerFromImage(selected, "Pasted", image)
                        else -> {
                            val x = MathUtil.clip(0, master.frameManager.workView!!.offsetX, workspace.width - image.width)
                            val y = MathUtil.clip(0, master.frameManager.workView!!.offsetY, workspace.height - image.height)
                            workspace.selectionEngine.setSelectionWithLifted(
                                    Selection.RectangleSelection(Rect(x, y, image.width, image.height)),
                                    LiftedImageData(image))
                        }
                    }
                }
            }
            PASTE_AS_LAYER.string -> {
                val image = Hybrid.imageIO.imageFromClipboard() ?: return false
                val workspace = master.workspaceSet.currentWorkspace
                if( workspace == null)
                    master.workspaceFromImage(image)
                else
                    workspace.groupTree.addSimpleLayerFromImage(workspace.groupTree.selectedNode, "Pasted", image)
            }

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: global.$string")
        }
        return true
    }

    private fun copyVisible(workspace: IImageWorkspace) : RawImage {
        val workspaceImage = master.renderEngine.renderWorkspace(workspace)

        return workspace.selectionEngine.selection?.lift(workspaceImage, null) ?: workspaceImage.deepCopy()
    }

    private fun layerFromImage( workspace: IImageWorkspace, context: GroupNode?, image: IImage) {

    }

    private fun copy(workspace: IImageWorkspace, cut: Boolean)  {

        val selectionEngine = workspace.selectionEngine

        val node = workspace.groupTree.selectedNode

        val image = when( node) {
            null -> copyVisible(workspace)
            else -> {
                val selection = selectionEngine.selection
                when( selection) {
                    null -> {
                        if( cut) (workspace.activeDrawer as? IClearModule)?.clear() ?: Hybrid.beep()
                        master.renderEngine.pullImage(RenderTarget(getRenderSourceForNode(node,workspace)))
                    }
                    else -> {
                        val liftedData = selectionEngine.liftedData
                        when( liftedData) {
                            null -> {
                                // TODO: Copy to multiple data Flavors: an image one based on the renderer (what
                                //  it is now) and a Spirite one based on the ILiftedData
                                val source = getRenderSourceForNode(node, workspace)
                                val x : Int
                                val y: Int
                                if( source is LayerSource) {
                                    x = source.layer.x
                                    y = source.layer.y
                                }
                                else {
                                    x = 0
                                    y = 0
                                }

                                // Flushed by the end of the function
                                val img2 = using( master.renderEngine.pullImage(RenderTarget(source))) {img->
                                    Hybrid.imageIO.imageToClipboard(img)

                                    // Flushed by the end of the function
                                    val img2 = selection.mask.deepCopy()
                                    val gc = img2.graphics
                                    gc.transform = Transform.TranslationMatrix(-x.f,-y.f) * (selection.transform?.invert() ?: Transform.IdentityMatrix)
                                    gc.composite = SRC_IN
                                    gc.renderImage(img, 0, 0)

                                    img2
                                }

                                if( cut) (workspace.activeDrawer as? IClearModule)?.clear() ?: Hybrid.beep()

                                img2
                            }
                            else -> {
                                // Flushed by the end of the function
                                val img = Hybrid.imageCreator.createImage(liftedData.width, liftedData.height)
                                        .also{ liftedData.draw(it.graphics)}

                                if( cut)
                                    selectionEngine.clearLifted()

                                img
                            }
                        }
                    }
                }

            }
        }

        using(image) {
            Hybrid.imageIO.imageToClipboard(image)
        }
    }
}