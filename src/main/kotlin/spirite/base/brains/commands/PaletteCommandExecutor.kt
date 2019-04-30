package spirite.base.brains.commands

import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.PaletteCommandExecutor.PaletteCommand.SWAP
import spirite.base.brains.commands.PaletteCommandExecutor.PaletteCommand.SWAP_BACK
import spirite.base.brains.palette.IPaletteManager
import spirite.hybrid.MDebug

class PaletteCommandExecutor(val paletteManager: IPaletteManager) : ICommandExecutor
{
    enum class PaletteCommand(val string: String) : ICommand {
        SWAP( "swap"),
        SWAP_BACK("swapBack"),
        ;

        override val commandString: String get() = "palette.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }

    override val validCommands: List<String> get() = PaletteCommand.values().map { it.string }
    override val domain: String get() = "palette"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            SWAP.string -> paletteManager.activeBelt.cycleColors(1)
            SWAP_BACK.string -> paletteManager.activeBelt.cycleColors(-1)

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: palette.$string")
        }
        return true
    }
}