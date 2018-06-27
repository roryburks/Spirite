package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand.*
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.util.f
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.hybrid.Hybrid

class DrawCommandExecutor(val workspaceSet: IWorkspaceSet, val toolsetManager: IToolsetManager) : ICommandExecuter
{

    enum class DrawCommand(val string: String) : ICommand {
        UNDO( "undo"),
        REDO("redo"),
        QUICK_NEW_LAYER("quickNewLayer"),
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
            QUICK_NEW_LAYER.string -> workspace.groupTree.addNewSimpleLayer(workspace.groupTree.selectedNode, "New Layer", DYNAMIC)
            CROP_SELECTION.string -> TODO()
            APPLY_TRANFORM.string -> {
                val reshape = toolsetManager.toolset.Reshape
                val transform = reshape.transform
                (workspace.activeDrawer as? ITransformModule)?.transform( transform)
                reshape.translation = Vec2(0f,0f)
                reshape.scale = Vec2(1f,1f)
                reshape.rotation = 0f
                workspace.selectionEngine.proposingTransform = null
            }
            AUTO_CROP.string -> TODO()
            LAYER_TO_IMAGE_SIZE.string -> TODO()
            INVERT.string -> (workspace.activeDrawer as? IInvertModule)?.invert() ?: Hybrid.beep()
            CLEAR.string ->  (workspace.activeDrawer as? IClearModule)?.clear() ?: Hybrid.beep()
            SHIFT_UP.string -> if( !shift(0,-1, workspace)) Hybrid.beep()
            SHIFT_DOWN.string -> if( !shift(0,1, workspace)) Hybrid.beep()
            SHIFT_LEFT.string -> if( !shift(-1,0, workspace)) Hybrid.beep()
            SHIFT_RIGHT.string -> if( !shift(1,0, workspace)) Hybrid.beep()
        }

        return true
    }

    fun shift( ox: Int, oy: Int, workspace: IImageWorkspace) : Boolean {
        val active = workspace.activeDrawer as? ITransformModule ?: return false
        active.transform(Transform.TranslationMatrix(ox.f, oy.f))
        return true
    }

}