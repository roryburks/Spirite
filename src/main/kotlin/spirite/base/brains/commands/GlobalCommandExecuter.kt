package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.*
import spirite.base.graphics.GraphicsContext.Composite.DST_IN
import spirite.base.graphics.GraphicsContext.Composite.SRC_IN
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.RenderTarget
import spirite.base.graphics.rendering.sources.LayerSource
import spirite.base.graphics.rendering.sources.getRenderSourceForNode
import spirite.base.imageData.IImageWorkspace
import spirite.base.util.f
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.gui.components.dialogs.IDialog.FilePickType
import spirite.gui.components.dialogs.IDialog.FilePickType.SAVE_SIF
import spirite.hybrid.Hybrid
import java.io.File

class GlobalCommandExecuter(val master: IMasterControl) : ICommandExecuter {
    enum class GlobalCommand(val string: String) : ICommand {
        PING( "ping"),
        SAVE_WORKSPACE("saveWorkspace"),
        SAVE_WORKSPACE_AS("saveWorkspaceAs"),
        OPEN("open"),
        EXPORT("export"),
        EXPORT_AS("export_as"),
        COPY("copy"),
        COPY_VISIBLE("copyVisible"),
        ;

        override val commandString: String get() = "global.$string"
    }

    override val validCommands: List<String> = GlobalCommand.values().map { it.string }
    override val domain: String get() = "global"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            PING.string -> println("PING")
            SAVE_WORKSPACE.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return true
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
                val workspace = master.workspaceSet.currentWorkspace ?: return true
                val file = master.dialog.pickFile(SAVE_SIF) ?: return true
                master.fileManager.saveWorkspace(workspace, file)
            }
            OPEN.string -> master.fileManager.openFile(master.dialog.pickFile(FilePickType.OPEN) ?: return true)
            EXPORT.string,
            EXPORT_AS.string -> master.fileManager.exportToImage(
                    master.workspaceSet.currentWorkspace ?: return true,
                    master.dialog.pickFile(FilePickType.EXPORT) ?: return true)
            COPY.string -> {
                val workspace = master.workspaceSet.currentWorkspace?: return true
                val selectionEngine = workspace.selectionEngine

                val node = workspace.groupTree.selectedNode

                val image = when( node) {
                    null -> copyVisible(workspace)
                    else -> {
                        val selection = selectionEngine.selection
                        when( selection) {
                            null -> master.renderEngine.renderImage(RenderTarget(getRenderSourceForNode(node,workspace)))
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

                                        val img = master.renderEngine.renderImage(RenderTarget(source), false)
                                        Hybrid.imageIO.imageToClipboard(img)

                                        val img2 = selection.mask.deepCopy()
                                        val gc = img2.graphics
                                        gc.transform = Transform.TranslationMatrix(-x.f,-y.f) * (selection.transform?.invert() ?: Transform.IdentityMatrix)
                                        gc.composite = DST_IN
                                        gc.renderImage(img, 0, 0)

                                        img.flush()
                                        img2
                                    }
                                    else -> Hybrid.imageCreator.createImage(liftedData.width, liftedData.height)
                                                .also{ liftedData.draw(it.graphics)}
                                }
                            }
                        }

                    }
                }
                Hybrid.imageIO.imageToClipboard(image)
                image.flush()
            }
            COPY_VISIBLE.string -> {
                val workspace = master.workspaceSet.currentWorkspace ?: return true
                Hybrid.imageIO.imageToClipboard(copyVisible(workspace))
            }
            else -> return false
        }
        return true
    }

    private fun copyVisible(workspace: IImageWorkspace) : RawImage {
        val workspaceImage = master.renderEngine.renderWorkspace(workspace)

        return workspace.selectionEngine.selection?.lift(workspaceImage, null) ?: workspaceImage.deepCopy()
    }

}