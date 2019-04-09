package spirite.base.brains

import rb.extendo.dataStructures.MutableOneToManyMap
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.GlobalCommands
import spirite.base.brains.commands.IsolationCommandExecuter.IsolationCommand
import spirite.base.brains.commands.NodeCommands
import spirite.base.brains.commands.PaletteCommandExecuter.PaletteCommand
import spirite.base.brains.commands.SelectCommand
import spirite.base.brains.commands.ToolsetCommandExecuter.ToolCommand
import spirite.base.brains.commands.WorkspaceCommandExecuter.ViewCommand
import spirite.base.brains.settings.IPreferences
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

data class Hotkey(
        val key: Int,
        val modifier: Int
)

class KeyCommand(
        val commandString : String,
        val objectCreator: ((IMasterControl)->Any?)?  = null
)
{
    // region generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyCommand
        if (commandString != other.commandString) return false
        return true
    }

    override fun hashCode(): Int {
        return commandString.hashCode()
    }
    // endregion
}

interface IHotkeyManager {
    fun getCommand( key: Hotkey) : KeyCommand?
    fun getHotkeysForCommand( command: String) : List<Hotkey>?

    fun setCommand( key: Hotkey, command : KeyCommand)
    fun removeHotkey( key: Hotkey)
}

private val defaultHotkeys = mapOf(
        ViewCommand.ZOOM_IN.keyCommand to (Hotkey( KeyEvent.VK_ADD, 0)),
        ViewCommand.ZOOM_OUT.keyCommand to (Hotkey( KeyEvent.VK_SUBTRACT, 0)),
        ViewCommand.ZOOM_IN_SLOW.keyCommand to (Hotkey( KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK)),
        ViewCommand.ZOOM_OUT_SLOW.keyCommand to (Hotkey( KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK)),
        ViewCommand.ZOOM_0.keyCommand to (Hotkey( KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK)),

        ToolCommand.Pen.keyCommand to (Hotkey( KeyEvent.VK_B, 0)),
        ToolCommand.Eraser.keyCommand to (Hotkey( KeyEvent.VK_E, 0)),
        ToolCommand.Fill.keyCommand to (Hotkey( KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK)),
        ToolCommand.ShapeSelection.keyCommand to (Hotkey( KeyEvent.VK_R, 0)),
        ToolCommand.Move.keyCommand to (Hotkey( KeyEvent.VK_T, 0)),
        ToolCommand.ColorPicker.keyCommand to (Hotkey( KeyEvent.VK_O, 0)),
        ToolCommand.Pixel.keyCommand to (Hotkey( KeyEvent.VK_A, 0)),
        ToolCommand.Rigger.keyCommand to (Hotkey( KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK)),

        ToolCommand.DecreasePenSize.keyCommand to (Hotkey( KeyEvent.VK_OPEN_BRACKET, 0)),
        ToolCommand.IncreasePenSize.keyCommand to (Hotkey( KeyEvent.VK_CLOSE_BRACKET, 0)),

        PaletteCommand.SWAP.keyCommand to (Hotkey( KeyEvent.VK_X, 0)),
        PaletteCommand.SWAP_BACK.keyCommand to (Hotkey( KeyEvent.VK_Z, 0)),

        NodeCommands.QuickNewLayer.keyCommand to (Hotkey( KeyEvent.VK_INSERT, 0)),
        NodeCommands.NewSpriteLayer.keyCommand to Hotkey( KeyEvent.VK_INSERT, InputEvent.SHIFT_DOWN_MASK),
        NodeCommands.NewGroup.keyCommand to (Hotkey(KeyEvent.VK_INSERT, InputEvent.CTRL_DOWN_MASK)),
        NodeCommands.MoveDown.keyCommand to (Hotkey(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK)),
        NodeCommands.MoveUp.keyCommand to (Hotkey(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK)),
        NodeCommands.Duplicate.keyCommand to (Hotkey(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK)),

        IsolationCommand.TOGGLE_ISOLATION.keyCommand to (Hotkey(KeyEvent.VK_BACK_QUOTE, 0)),
        IsolationCommand.ISOLATE_LAYER.keyCommand to (Hotkey(KeyEvent.VK_BACK_QUOTE, InputEvent.CTRL_DOWN_MASK)),
        IsolationCommand.CLEAR_ALL_ISOLATION.keyCommand to (Hotkey(KeyEvent.VK_BACK_QUOTE, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        DrawCommand.UNDO.keyCommand to (Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)),
        DrawCommand.REDO.keyCommand to (Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        DrawCommand.CLEAR.keyCommand to (Hotkey( KeyEvent.VK_DELETE, 0)),
        DrawCommand.INVERT.keyCommand to (Hotkey( KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK )),
        //KeyCommand("draw.toggle_reference") to (Hotkey( KeyEvent.VK_BACK_QUOTE, 0)),
        KeyCommand("draw.lift_to_reference") to (Hotkey( KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        DrawCommand.SHIFT_LEFT.keyCommand to (Hotkey( KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        DrawCommand.SHIFT_RIGHT.keyCommand to (Hotkey( KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        DrawCommand.SHIFT_DOWN.keyCommand to (Hotkey( KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        DrawCommand.SHIFT_UP.keyCommand to (Hotkey( KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)),
        DrawCommand.SCALE3x.keyCommand to (Hotkey( KeyEvent.VK_3, 0)),

        SelectCommand.All.keyCommand to (Hotkey( KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK)),
        SelectCommand.None.keyCommand to (Hotkey( KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        SelectCommand.Invert.keyCommand to (Hotkey( KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        SelectCommand.LiftInPlace.keyCommand to (Hotkey( KeyEvent.VK_X, InputEvent.SHIFT_DOWN_MASK)),

        GlobalCommands.SaveWorkspace.keyCommand to (Hotkey( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)),
        GlobalCommands.Copy.keyCommand to (Hotkey( KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)),
        GlobalCommands.CopyVisible.keyCommand to (Hotkey( KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        GlobalCommands.Paste.keyCommand to (Hotkey( KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)),
        GlobalCommands.PasteAsLayer.keyCommand to (Hotkey( KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        GlobalCommands.Cut.keyCommand to (Hotkey( KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)),
        GlobalCommands.Open.keyCommand to (Hotkey( KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)),
        GlobalCommands.NewWorkspace.keyCommand to (Hotkey( KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)),
☺
        // DEBUG
        GlobalCommands.PurgeUndoHistory.keyCommand to (Hotkey(KeyEvent.VK_F11, InputEvent.ALT_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),
        GlobalCommands.CopyAllLayer.keyCommand to (Hotkey(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        // TODO: This should really be animation., but that might require restructuring/rethinking of
        //	command execution system.

        KeyCommand("global.debug1") to (Hotkey( KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)),

        KeyCommand("frame.focus.LAYER_PROPERTIES") to (Hotkey(KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK)))

class HotkeyManager(
        val preferences: IPreferences
) : IHotkeyManager {
    private val hotkeyMap = MutableOneToManyMap<KeyCommand, Hotkey>()

    init {
        // Load either Default or saved preferences
        defaultHotkeys.entries.forEach {
            val command = it.key
            val defaultHotkey = it.value

            val preferenceString = preferences.getString(command.commandString)
            if( preferenceString == null) {
                hotkeyMap.assosciate(defaultHotkey, command)
            }
            else if( !preferenceString.isBlank()) {
                preferenceStringToHotkeys(preferenceString)
                        .forEach { hotkey -> hotkeyMap.assosciate(hotkey, command) }
            }
        }
    }

    override fun getCommand(key: Hotkey): KeyCommand? = hotkeyMap.getOne(key)
    override fun getHotkeysForCommand(command: String): List<Hotkey>? = hotkeyMap.getMany(KeyCommand(command))
    override fun setCommand(key: Hotkey, command: KeyCommand) {
        val oldCommand = hotkeyMap.getOne(key)

        hotkeyMap.assosciate(key, command)

        // Change Old
        if( oldCommand != null) {
            val oldMany = hotkeyMap.getMany(oldCommand)
            if( oldMany == null)
                preferences.putString(oldCommand.commandString,"")
            else
                preferences.putString(oldCommand.commandString, hotkeysToPreferenceString(oldMany))
        }

        // Change New
        val newCommandsRewritten = hotkeyMap.getMany(command)
        if( newCommandsRewritten != null)
            preferences.putString(command.commandString, hotkeysToPreferenceString(newCommandsRewritten))

    }
    override fun removeHotkey(key: Hotkey) {
        val command = hotkeyMap.many_to_one[key]
        hotkeyMap.dissociate(key)

        if( command != null) {
            val hotkeysToRewrite = hotkeyMap.getMany( command)
            if( hotkeysToRewrite == null)
                preferences.putString(command.commandString,"")
            else
                preferences.putString(command.commandString, hotkeysToPreferenceString(hotkeysToRewrite))
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

