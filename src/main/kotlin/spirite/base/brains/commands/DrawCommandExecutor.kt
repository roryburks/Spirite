package spirite.base.brains.commands

import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import spirite.sguiHybrid.MDebug
import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand.*
import spirite.base.brains.commands.specific.LayerFixes.bakeOffset
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.groupTree.LayerNode

class DrawCommandExecutor(val workspaceSet: IWorkspaceSet, val toolsetManager: IToolsetManager) : ICommandExecutor
{

    enum class DrawCommand(val string: String) : ICommand {
        UNDO( "undo"),
        REDO("redo"),
        CROP_SELECTION("cropSelection"),
        APPLY_TRANFORM("applyTranform"),
        AUTO_CROP("autoCrop"),
        LAYER_TO_IMAGE_SIZE("layerToImageSize"),
        INVERT("invert"),
        CLEAR("clear"),
        SHIFT_UP("shiftUp"),
        SHIFT_DOWN("shiftDown"),
        SHIFT_LEFT("shiftLeft"),
        SHIFT_RIGHT("shiftRight"),
        SCALE3x("Scale3x")
        ;

        override val commandString: String get() = "draw.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
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
            APPLY_TRANFORM.string -> {
                val reshape = toolsetManager.toolset.Reshape
                val transform = reshape.transform
                (workspace.activeDrawer as? ITransformModule)?.transform( transform)
                reshape.translation = Vec2f(0f,0f)
                reshape.scale = Vec2f(1f,1f)
                reshape.rotation = 0f
                workspace.selectionEngine.proposingTransform = null
            }
            AUTO_CROP.string -> TODO()
            LAYER_TO_IMAGE_SIZE.string -> TODO()
            INVERT.string -> (workspace.activeDrawer as? IInvertModule)?.invert() ?: return false
            CLEAR.string ->  (workspace.activeDrawer as? IClearModule)?.clear() ?: return false
            SHIFT_UP.string -> if( !shift(0,-1, workspace)) return false
            SHIFT_DOWN.string -> if( !shift(0,1, workspace)) return false
            SHIFT_LEFT.string -> if( !shift(-1,0, workspace)) return false
            SHIFT_RIGHT.string -> if( !shift(1,0, workspace)) return false
            SCALE3x.string -> {
                bakeOffset(workspace, workspace.groupTree.selectedNode as? LayerNode ?: return false)
                //val transform = MutableTransformF.Scale(1.1f,1.1f)
                //LayerFixes.ApplyTransformAccrossNode(workspace, workspace.groupTree.selectedNode ?: return false, transform)
            }

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: draw.$string")
        }

        return true
    }

    fun shift( ox: Int, oy: Int, workspace: IImageWorkspace) : Boolean {
        val active = workspace.activeDrawer as? ITransformModule ?: return false
        active.transform(ImmutableTransformF.Translation(ox.f, oy.f))
        return true
    }

}