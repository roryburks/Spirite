package spirite.base.brains

import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand
import spirite.base.brains.commands.ToolsetCommandExecuter.ToolCommand
import spirite.base.brains.commands.WorkspaceCommandExecuter.ViewCommand
import spirite.base.brains.settings.IPreferences
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
        ViewCommand.ZOOM_IN.commandString to (Hotkey( KeyEvent.VK_ADD, 0)),
        ViewCommand.ZOOM_OUT.commandString to (Hotkey( KeyEvent.VK_SUBTRACT, 0)),
        ViewCommand.ZOOM_IN_SLOW.commandString to (Hotkey( KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK)),
        ViewCommand.ZOOM_OUT_SLOW.commandString to (Hotkey( KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK)),
        ViewCommand.ZOOM_0.commandString to (Hotkey( KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK)),

        ToolCommand.Pen.commandString to (Hotkey( KeyEvent.VK_P, 0)),
        ToolCommand.Eraser.commandString to (Hotkey( KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK)),
        ToolCommand.Fill.commandString to (Hotkey( KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK)),
        ToolCommand.ShapeSelection.commandString to (Hotkey( KeyEvent.VK_R, 0)),
        ToolCommand.Move.commandString to (Hotkey( KeyEvent.VK_M, 0)),
        ToolCommand.ColorPicker.commandString to (Hotkey( KeyEvent.VK_O, 0)),
        ToolCommand.Pixel.commandString to (Hotkey( KeyEvent.VK_A, 0)),
        ToolCommand.Rigger.commandString to (Hotkey( KeyEvent.VK_CAPS_LOCK, 0)),

        "palette.swap" to (Hotkey( KeyEvent.VK_X, 0)),
        "palette.swapBack" to (Hotkey( KeyEvent.VK_Z, 0)),

        "draw.newLayerQuick" to (Hotkey( KeyEvent.VK_INSERT, 0)),
        DrawCommand.UNDO.commandString to (Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)),
        DrawCommand.REDO.commandString to (Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
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

        GlobalCommand.SAVE_WORKSPACE.commandString to (Hotkey( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)),
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

