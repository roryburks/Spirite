package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.*
import spirite.base.graphics.RawImage
import spirite.base.graphics.rendering.RenderTarget
import spirite.base.graphics.rendering.sources.getRenderSourceForNode
import spirite.base.imageData.IImageWorkspace
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
                                    null -> {Hybrid.beep();return false}
                                    else -> Hybrid.imageCreator.createImage(liftedData.width, liftedData.height)
                                                .also{ liftedData.draw(it.graphics)}
                                }
                            }
                        }

                    }
                }
                Hybrid.imageIO.imageToClipboard(image)

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