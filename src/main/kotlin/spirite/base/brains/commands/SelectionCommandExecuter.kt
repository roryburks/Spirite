package spirite.base.brains.commands

import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.commands.SelectionCommandExecuter.SelectCommand.*
import spirite.base.imageData.selection.Selection
import spirite.base.imageData.selection.Selection.Companion
import spirite.base.util.linear.Rect

class SelectionCommandExecuter( val workspaceSet: IWorkspaceSet) : ICommandExecuter
{
    val workspace get() = workspaceSet.currentWorkspace

    enum class SelectCommand( val string: String) : ICommand {
        ALL("all"),
        NONE("none"),
        INVERT("invert")
        ;

        override val commandString: String get() = "select.$string"
    }

    override val validCommands: List<String> get() = SelectCommand.values().map { it.string }
    override val domain: String get() = "select"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        val workspace = workspace ?: return validCommands.contains(string)
        val selectionEngine = workspace.selectionEngine

        when( string) {
            ALL.string -> selectionEngine.setSelection(Selection.RectangleSelection(Rect(workspace.width, workspace.height)) )
            NONE.string -> selectionEngine.setSelection(null)
            INVERT.string -> selectionEngine.setSelection(
                    selectionEngine.selection?.invert(workspace.width, workspace.height)
                            ?: Selection.RectangleSelection(Rect(workspace.width, workspace.height)))
            else -> return false
        }
        return true
    }
}