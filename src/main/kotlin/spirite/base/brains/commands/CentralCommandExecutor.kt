package spirite.base.brains.commands

import rb.extendo.dataStructures.Deque
import rb.extendo.extensions.toHashMap
import spirite.sguiHybrid.Hybrid
import spirite.sguiHybrid.MDebug
import spirite.base.brains.IMasterControl
import spirite.base.brains.MWorkspaceSet
import spirite.gui.menus.dialogs.IDialog

interface ICentralCommandExecutor {
    val commandDomains : List<String>
    val validCommands: List<String>
    fun executeCommand( command: String, extra: Any?) : Boolean

    data class CommandPair(val command: String, val extra: String?)
    val executedCommands : Iterable<CommandPair>
}

class CentralCommandExecutor(
        val master: IMasterControl,
        val workspaceSet: MWorkspaceSet,
        val dialog: IDialog,
        private val _maxHistorySize : Int? = 3000)
    : ICentralCommandExecutor
{
    private val commandExecutors = listOf(
            NodeContextCommand(workspaceSet, dialog),
            DrawCommandExecutor(workspaceSet, master.toolsetManager),
            GlobalCommandExecutor(master, workspaceSet),
            WorkViewCommandExecutor(master),
            ToolsetCommandExecutor(master.toolsetManager),
            PaletteCommandExecutor(master.paletteManager, master.topLevelFeedbackSystem, master.workspaceSet),
            SelectionCommandExecutor(workspaceSet),
            FrameCommandExecutor(master.frameManager),
            IsolationCommandExecutor(workspaceSet),
            AnimationCommandExecutor(master),
            WorkspaceCommandExecutor(workspaceSet, dialog),
            SpriteLayerCommandExecutor(master),
            DebugCommandExecutor(master)
            )
            .toHashMap { it.domain }


    override val commandDomains: List<String> get() = commandExecutors.keys.toList()
    override val validCommands: List<String> get() = commandExecutors.values.flatMap { it.validCommands }

    override fun executeCommand(command: String, extra: Any?) : Boolean {
        if( _maxHistorySize != null && executedCommands.length >= _maxHistorySize)
            executedCommands.popBack()
        executedCommands.addFront(ICentralCommandExecutor.CommandPair(command, extra?.toString()))

        var executed = false
        Hybrid.gle.runInGLContext {
            val space = command.substring(0, command.indexOf("."))
            val subCommand = command.substring(space.length + 1)

            var attempted = false

            commandExecutors[space]?.run {
                attempted = true
                if (executeCommand(subCommand, extra))
                    executed = true
            }

            if (!attempted) {
                MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command domain: $space")
            }
        }
        return executed
    }

    override val executedCommands = Deque<ICentralCommandExecutor.CommandPair>()
}