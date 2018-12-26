package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.KeyCommand
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.selection.Selection
import spirite.base.util.linear.Rect

class SelectionCommandExecuter( val workspaceSet: IWorkspaceSet) : ICommandExecuter
{
    val workspace get() = workspaceSet.currentWorkspace

    override val validCommands: List<String> get() = executers.map { "$domain.${it.name}" }
    override val domain: String get() = "select"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        val workspace = workspace ?: return validCommands.contains(string)
        executers.asSequence()
                .firstOrNull { it.name == string }
                ?.execute(workspace)
        return true
    }
}

private val executers = mutableListOf<SelectionCommand>()
abstract class SelectionCommand : ICommand {
    // Note: this is somewhat of a hacky way to make sure each AnimationCommand added gets added to the list
    init {executers.add(this)}

    abstract val name: String
    abstract fun execute(workspace: IImageWorkspace)

    override val commandString: String get() = "select.$name"
    override val keyCommand: KeyCommand
        get() = KeyCommand(commandString) {it.workspaceSet.currentWorkspace?.animationManager?.currentAnimation}
}

object SelectCommand {
    object All : SelectionCommand() {
        override val name: String get() = "all"
        override fun execute(workspace: IImageWorkspace) {
            workspace.selectionEngine.setSelection(Selection.RectangleSelection(Rect(workspace.width, workspace.height)))
        }
    }

    object None : SelectionCommand() {
        override val name: String get() = "none"
        override fun execute(workspace: IImageWorkspace) {
            workspace.selectionEngine.setSelection(null)
        }
    }

    object Invert : SelectionCommand() {
        override val name: String get() = "invert"
        override fun execute(workspace: IImageWorkspace) {
            workspace.selectionEngine.setSelection(
                    workspace.selectionEngine.selection?.invert(workspace.width, workspace.height)
                            ?: Selection.RectangleSelection(Rect(workspace.width, workspace.height)))
        }
    }

    object LiftInPlace : SelectionCommand() {
        override val name: String get() = "lift"
        override fun execute(workspace: IImageWorkspace) {
            workspace.selectionEngine.attemptLiftData(workspace.activeDrawer)
        }
    }
}