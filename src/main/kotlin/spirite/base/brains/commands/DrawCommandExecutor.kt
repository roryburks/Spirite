package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand.*
import spirite.base.imageData.groupTree.GroupTree.Node

class DrawCommandExecutor(val workspaceSet: IWorkspaceSet) : ICommandExecuter
{

    enum class DrawCommand(val string: String) : ICommand {
        UNDO( "undo"),
        REDO("redo"),
        CROP_SELECTION("cropSelection"),
        APPLY_TRANFORM("applyTranform"),
        AUTO_CROP("autoCrop"),
        LAYER_TO_IMAGE_SIZE("layerToImageSize")
        ;

        override val commandString: String get() = "draw.$string"
    }

    override val validCommands: List<String> get() = DrawCommand.values().map {  it.string }
    override val domain: String get() = "draw"
    val currentWorskapce get() = workspaceSet.currentWorkspace

    override fun executeCommand(string: String, extra: Any?) : Boolean{
        val workspace = currentWorskapce ?: return false
        val node = extra as? Node
        when(string) {
            UNDO.string -> workspace.undoEngine.undo()
            REDO.string -> workspace.undoEngine.redo()
            CROP_SELECTION.string -> TODO()
            APPLY_TRANFORM.string -> TODO()
            AUTO_CROP.string -> TODO()
            LAYER_TO_IMAGE_SIZE.string -> TODO()
        }

        return true
    }

}