package spirite.base.brains.commands

import spirite.base.brains.ITopLevelFeedbackSystem
import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.KeyCommand
import spirite.base.brains.palette.IPaletteManager
import spirite.base.exceptions.CommandNotValidException
import spirite.base.imageData.IImageWorkspace

class PaletteCommandExecutor(
        val paletteManager: IPaletteManager,
        val topLevelFeedbackSystem: ITopLevelFeedbackSystem,
        val workspaceSet: IWorkspaceSet)
    : ICommandExecutor
{

    override val validCommands get() = commands.values.map { it.commandString }
    override val domain: String get() = "palette"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        try
        {
            commands[string]?.action?.invoke(PaletteCommandContext(paletteManager, topLevelFeedbackSystem, workspaceSet.currentWorkspace)) ?: return false
            return true
        }catch (e: CommandNotValidException)
        {
            return false
        }
    }
}

// region Glue
private val commands = HashMap<String,PaletteCommand>()
private class PaletteCommand
constructor(
        val name: String,
        val action: (paletteManager: PaletteCommandContext)->Unit)
    :ICommand
{
    init {commands[name] = this}

    override val commandString: String get() = "palette.$name"
    override val keyCommand: KeyCommand get() = KeyCommand(commandString)
}

private class PaletteCommandContext(
        val paletteManager: IPaletteManager,
        val topLevelFeedbackSystem: ITopLevelFeedbackSystem,
        val currentWorkspace: IImageWorkspace?)
// endregion

object PaletteCommands
{
    val Swap : ICommand = PaletteCommand("swap") {it.paletteManager.activeBelt.cycleColors(1)}
    val SwapBack : ICommand = PaletteCommand("swapBack") {it.paletteManager.activeBelt.cycleColors(-1)}

    val SwitchModes: ICommand = PaletteCommand("switchModes") {
        when( it.paletteManager.drivePalette) {
            true -> {
                it.topLevelFeedbackSystem.broadcastGeneralMessage("Tracking Palette Manager: Off")
                it.paletteManager.drivePalette = false
            }
            false -> {
                it.topLevelFeedbackSystem.broadcastGeneralMessage("Tracking Palette Manager: On")
                it.paletteManager.drivePalette = true
            }
        }
    }

    val CyclePalettes: ICommand = PaletteCommand("cyclePalette"){
        val ws = it.currentWorkspace ?: throw CommandNotValidException
        val currentPalette = it.paletteManager.currentPalette
        val currentPaletteId = ws.paletteSet.palettes.indexOf(currentPalette)
        val newPaletteId = (currentPaletteId + 1) % ws.paletteSet.palettes.size
        it.paletteManager.currentPalette = ws.paletteSet.palettes.getOrNull( newPaletteId) ?: it.paletteManager.globalPalette
    }
}