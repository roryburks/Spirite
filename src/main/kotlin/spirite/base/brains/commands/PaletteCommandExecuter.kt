package spirite.base.brains.commands

import spirite.base.brains.commands.PaletteCommandExecuter.PaletteCommand.*
import spirite.base.brains.palette.IPaletteManager

class PaletteCommandExecuter(val paletteManager: IPaletteManager) : ICommandExecuter
{
    enum class PaletteCommand(val string: String) : ICommand {
        SWAP( "swap"),
        SWAP_BACK("swapBack"),
        ;

        override val commandString: String get() = "palette.$string"
    }

    override val validCommands: List<String> get() = PaletteCommand.values().map { it.string }
    override val domain: String get() = "palette"

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when( string) {
            SWAP.string -> paletteManager.cycleActiveColors(1)
            SWAP_BACK.string -> paletteManager.cycleActiveColors(-1)
            else -> return false
        }
        return true
    }
}