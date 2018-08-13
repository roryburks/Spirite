package spirite.base.brains.commands

import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet
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
    val commandExecuters : List<ICommandExecuter> = listOf(
            NodeContextCommand(workspaceSet, dialog),
            DrawCommandExecutor(workspaceSet, master.toolsetManager),
            GlobalCommandExecuter(master),
            WorkspaceCommandExecuter(master),
            ToolsetCommandExecuter(master.toolsetManager),
            PaletteCommandExecuter(master.paletteManager),
            SelectionCommandExecuter(workspaceSet),
            FrameCommandExecuter(master.frameManager),
            IsolationCommandExecuter(workspaceSet)
    )


    override val commandDomains: List<String> get() = commandExecuters.map { it.domain }
    override val validCommands: List<String> get() = commandExecuters.fold(mutableListOf()) { agg, executor ->
        agg.addAll(executor.validCommands)
        agg
    }

    override fun executeCommand(command: String, extra: Any?) : Boolean {
        val space = command.substring(0, command.indexOf("."))
        val subCommand = command.substring(space.length+1)

        var attempted = false
        var executed = false

        // Note: It's probably a bad idea to have multiple executers with the same domain, but at least here it will be supported
        commandExecuters
                .filter { it.domain == space }
                .forEach {
                    attempted = true
                    if( it.executeCommand(subCommand, extra))
                        executed = true
                }

        if (!attempted) {
            MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command domain: $space")
        }

        return executed
    }
}