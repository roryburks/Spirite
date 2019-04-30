package spirite.base.brains.commands

import rb.extendo.extensions.toHashMap
import spirite.base.brains.IMasterControl
import spirite.base.brains.MWorkspaceSet
import spirite.gui.components.dialogs.IDialog
import spirite.hybrid.MDebug

interface ICentralCommandExecutor {
    val commandDomains : List<String>
    val validCommands: List<String>
    fun executeCommand( command: String, extra: Any?) : Boolean
}

class CentralCommandExecutor(
        val master: IMasterControl,
        val workspaceSet: MWorkspaceSet,
        val dialog: IDialog)
    : ICentralCommandExecutor
{
    private val commandExecutors = listOf(
            NodeContextCommand(workspaceSet, dialog),
            DrawCommandExecutor(workspaceSet, master.toolsetManager),
            GlobalCommandExecutor(master, workspaceSet),
            ViewCommandExecutor(master),
            ToolsetCommandExecutor(master.toolsetManager),
            PaletteCommandExecutor(master.paletteManager),
            SelectionCommandExecutor(workspaceSet),
            FrameCommandExecutor(master.frameManager),
            IsolationCommandExecutor(workspaceSet),
            AnimationCommandExecutor(master),
            WorkspaceCommandExecutor(workspaceSet, dialog))
            .toHashMap { it.domain }


    override val commandDomains: List<String> get() = commandExecutors.keys.toList()
    override val validCommands: List<String> get() = commandExecutors.values.flatMap { it.validCommands }

    override fun executeCommand(command: String, extra: Any?) : Boolean {
        val space = command.substring(0, command.indexOf("."))
        val subCommand = command.substring(space.length+1)

        var attempted = false
        var executed = false

        commandExecutors[space]?.run {
            attempted = true
            if( executeCommand(subCommand, extra))
                executed = true
        }

        if (!attempted) {
            MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command domain: $space")
        }

        return executed
    }
}