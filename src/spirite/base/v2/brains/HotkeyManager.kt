package spirite.base.v2.brains

import spirite.base.v2.brains.Settings.IPreferences
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

data class Hotkey(
        val key: Int,
        val modifier: Int
)

interface IHotkeyManager {
    fun getCommand( key: Hotkey) : String?
    fun getHotkeysForCommand( command: String) : List<Hotkey>?

    fun setCommand( key: Hotkey, command : String?)
}

class HotkeyManager(
        val preferences: IPreferences
) : IHotkeyManager {

    private val hotkeyToCommand = mutableMapOf<Hotkey, String>()
    private val commandToHotkeys = mutableMapOf<String, List<Hotkey>>()


    override fun getCommand(key: Hotkey): String? = hotkeyToCommand[key]
    override fun getHotkeysForCommand(command: String): List<Hotkey>? = commandToHotkeys[command]

    override fun setCommand(key: Hotkey, command: String?) {
        // Remove any existing mapping of
    }
}

