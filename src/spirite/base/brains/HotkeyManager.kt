package spirite.base.brains

import spirite.base.brains.Settings.IPreferences
import spirite.base.util.dataContainers.MutableOneToManyMap
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

data class Hotkey(
        val key: Int,
        val modifier: Int
)

interface IHotkeyManager {
    fun getCommand( key: Hotkey) : String?
    fun getHotkeysForCommand( command: String) : List<Hotkey>?

    fun setCommand( key: Hotkey, command : String)
    fun removeHotkey( key: Hotkey)
}

private val defaultHotkeys = mapOf(
        "context.zoom_in" to (Hotkey( KeyEvent.VK_ADD, 0)),
        "context.zoom_out" to (Hotkey( KeyEvent.VK_SUBTRACT, 0)),
        "context.zoom_in_slow" to (Hotkey( KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK)),
        "context.zoom_out_slow" to (Hotkey( KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK)),
        "context.zoom_0" to (Hotkey( KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK)),

        "toolset.PEN" to (Hotkey( KeyEvent.VK_P, 0)),
        "toolset.ERASER" to (Hotkey( KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK)),
        "toolset.FILL" to (Hotkey( KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK)),
        "toolset.BOX_SELECTION" to (Hotkey( KeyEvent.VK_R, 0)),
        "toolset.MOVE" to (Hotkey( KeyEvent.VK_M, 0)),
        "toolset.COLOR_PICKER" to (Hotkey( KeyEvent.VK_O, 0)),
        "toolset.PIXEL" to (Hotkey( KeyEvent.VK_A, 0)),
        "toolset.COMPOSER" to (Hotkey( KeyEvent.VK_CAPS_LOCK, 0)),

        "palette.swap" to (Hotkey( KeyEvent.VK_X, 0)),
        "palette.swapBack" to (Hotkey( KeyEvent.VK_Z, 0)),

        "draw.newLayerQuick" to (Hotkey( KeyEvent.VK_INSERT, 0)),
        "draw.undo" to (Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)),
        "draw.redo" to (Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        "draw.clearLayer" to (Hotkey( KeyEvent.VK_DELETE, 0)),
        "draw.invert" to (Hotkey( KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK )),
        "draw.toggle_reference" to (Hotkey( KeyEvent.VK_BACK_QUOTE, 0)),
        "draw.lift_to_reference" to (Hotkey( KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        "draw.shiftLeft" to (Hotkey( KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        "draw.shiftRight" to (Hotkey( KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        "draw.shiftDown" to (Hotkey( KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        "draw.shiftUp" to (Hotkey( KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),

        "select.all" to (Hotkey( KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK)),
        "select.none" to (Hotkey( KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        "select.invert" to (Hotkey( KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        "global.save_image" to (Hotkey( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)),
        "global.copy" to (Hotkey( KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)),
        "global.copyVisible" to (Hotkey( KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        "global.paste" to (Hotkey( KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)),
        "global.pasteAsLayer" to (Hotkey( KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        "global.cut" to (Hotkey( KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)),

        // TODO: This should really be anim., but that might require restructuring/rethinking of
        //	command execution system.
        "draw.addGapQuick" to Hotkey( KeyEvent.VK_INSERT, InputEvent.SHIFT_DOWN_MASK),

        "global.debug1" to (Hotkey( KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        "frame.focus.LAYER_PROPERTIES" to (Hotkey(KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK)))

class HotkeyManager(
        val preferences: IPreferences
) : IHotkeyManager {
    private val hotkeyMap = MutableOneToManyMap<String, Hotkey>()

    init {
        // Load either Default or saved preferences
        defaultHotkeys.entries.forEach {
            val command = it.key
            val defaultHotkey = it.value

            val preferenceString = preferences.getString(command)
            if( preferenceString == null) {
                hotkeyMap.assosciate(defaultHotkey, command)
            }
            else if( !preferenceString.isBlank()) {
                preferenceStringToHotkeys(preferenceString)
                        .forEach { hotkey -> hotkeyMap.assosciate(hotkey, command) }
            }
        }
    }

    override fun getCommand(key: Hotkey): String?
            = hotkeyMap.getOne(key)
    override fun getHotkeysForCommand(command: String): List<Hotkey>?
            = hotkeyMap.getMany(command)
    override fun setCommand(key: Hotkey, command: String) {
        val oldCommand = hotkeyMap.getOne(key)

        hotkeyMap.assosciate(key, command)

        // Change Old
        if( oldCommand != null) {
            val oldMany = hotkeyMap.getMany(oldCommand)
            if( oldMany == null)
                preferences.putString(oldCommand,"")
            else
                preferences.putString(oldCommand, hotkeysToPreferenceString(oldMany))
        }

        // Change New
        val newCommandsRewritten = hotkeyMap.getMany(command)
        if( newCommandsRewritten != null)
            preferences.putString(command, hotkeysToPreferenceString(newCommandsRewritten))

    }
    override fun removeHotkey(key: Hotkey) {
        val command = hotkeyMap.many_to_one[key]
        hotkeyMap.dissociate(key)

        if( command != null) {
            val hotkeysToRewrite = hotkeyMap.getMany( command)
            if( hotkeysToRewrite == null)
                preferences.putString(command,"")
            else
                preferences.putString(command, hotkeysToPreferenceString(hotkeysToRewrite))
        }
    }

    private fun hotkeysToPreferenceString( keys: List<Hotkey>) : String {
        return keys.map { "${it.key},${it.modifier}" }
                .joinToString (";")
    }

    private fun preferenceStringToHotkeys( prefString : String) : List<Hotkey> {
        return prefString.split(";")
                .map {
                    val params = it.split(",")
                    Hotkey( params[0].toInt(10), params[1].toInt(10))
                }
    }
}

