package spirite.base.brains

import rb.extendo.dataStructures.MutableOneToManyMap
import spirite.base.brains.commands.*
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.IsolationCommandExecutor.IsolationCommand
import spirite.base.brains.commands.ToolsetCommandExecutor.ToolCommand
import spirite.base.brains.commands.WorkViewCommandExecutor.WorkViewCommand
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

private const val ALT = InputEvent.ALT_DOWN_MASK
private const val CTRL = InputEvent.CTRL_DOWN_MASK
private const val SHIFT = InputEvent.SHIFT_DOWN_MASK

private val defaultHotkeys = mapOf(
        WorkViewCommand.ZOOM_IN.keyCommand to (Hotkey( KeyEvent.VK_ADD, 0)),
        WorkViewCommand.ZOOM_OUT.keyCommand to (Hotkey( KeyEvent.VK_ADD, CTRL or SHIFT)),
        WorkViewCommand.ZOOM_IN_SLOW.keyCommand to (Hotkey( KeyEvent.VK_ADD, CTRL)),
        WorkViewCommand.ZOOM_OUT_SLOW.keyCommand to (Hotkey( KeyEvent.VK_SUBTRACT, CTRL)),
        WorkViewCommand.ZOOM_0.keyCommand to (Hotkey( KeyEvent.VK_NUMPAD0, CTRL)),

        ToolCommand.Pen.keyCommand to (Hotkey( KeyEvent.VK_B, 0)),
        ToolCommand.Eraser.keyCommand to (Hotkey( KeyEvent.VK_E, 0)),
        ToolCommand.Fill.keyCommand to (Hotkey( KeyEvent.VK_B, SHIFT)),
        ToolCommand.ShapeSelection.keyCommand to (Hotkey( KeyEvent.VK_R, 0)),
        ToolCommand.Move.keyCommand to (Hotkey( KeyEvent.VK_T, 0)),
        ToolCommand.ColorPicker.keyCommand to (Hotkey( KeyEvent.VK_O, 0)),
        ToolCommand.Pixel.keyCommand to (Hotkey( KeyEvent.VK_A, 0)),
        ToolCommand.Rigger.keyCommand to (Hotkey( KeyEvent.VK_R, SHIFT)),
        ToolCommand.MagneticFill.keyCommand to (Hotkey(KeyEvent.VK_F, 0)),
        ToolCommand.FreeSelection.keyCommand to (Hotkey(KeyEvent.VK_F, SHIFT)),

        ToolCommand.SetMode_1.keyCommand to (Hotkey(KeyEvent.VK_1, SHIFT)),
        ToolCommand.SetMode_2.keyCommand to (Hotkey(KeyEvent.VK_2, SHIFT)),
        ToolCommand.SetMode_3.keyCommand to (Hotkey(KeyEvent.VK_3, SHIFT)),
        ToolCommand.SetMode_4.keyCommand to (Hotkey(KeyEvent.VK_4, SHIFT)),

        ToolCommand.DecreasePenSize.keyCommand to (Hotkey( KeyEvent.VK_OPEN_BRACKET, 0)),
        ToolCommand.IncreasePenSize.keyCommand to (Hotkey( KeyEvent.VK_OPEN_BRACKET, CTRL or SHIFT)),

        PaletteCommands.Swap.keyCommand to (Hotkey( KeyEvent.VK_X, 0)),
        PaletteCommands.SwapBack.keyCommand to (Hotkey( KeyEvent.VK_Z, 0)),
        PaletteCommands.SwitchModes.keyCommand to (Hotkey(KeyEvent.VK_P, SHIFT or CTRL)),
        PaletteCommands.CyclePalettes.keyCommand to (Hotkey(KeyEvent.VK_P, CTRL)),

        NodeCommands.QuickNewLayer.keyCommand to (Hotkey( KeyEvent.VK_INSERT, 0)),
        NodeCommands.NewSpriteLayer.keyCommand to Hotkey( KeyEvent.VK_INSERT, SHIFT),
        NodeCommands.NewGroup.keyCommand to (Hotkey(KeyEvent.VK_INSERT, CTRL)),
        NodeCommands.NewPuppetLayer.keyCommand to (Hotkey(KeyEvent.VK_INSERT, CTRL or SHIFT)),
        NodeCommands.MoveDown.keyCommand to (Hotkey(KeyEvent.VK_DOWN, CTRL)),
        NodeCommands.MoveUp.keyCommand to (Hotkey(KeyEvent.VK_UP, CTRL)),
        NodeCommands.Duplicate.keyCommand to (Hotkey(KeyEvent.VK_U, CTRL)),

        WorkspaceCommands.ToggleView.keyCommand to (Hotkey(KeyEvent.VK_TAB,0)),
        WorkspaceCommands.ResetOtherView.keyCommand to (Hotkey(KeyEvent.VK_TAB, SHIFT)),
        WorkspaceCommands.ResetOtherView.keyCommand to (Hotkey(KeyEvent.VK_TAB, CTRL)),

        IsolationCommand.TOGGLE_ISOLATION.keyCommand to (Hotkey(KeyEvent.VK_BACK_QUOTE, 0)),
        IsolationCommand.ISOLATE_LAYER.keyCommand to (Hotkey(KeyEvent.VK_BACK_QUOTE, CTRL)),
        IsolationCommand.CLEAR_ALL_ISOLATION.keyCommand to (Hotkey(KeyEvent.VK_BACK_QUOTE, CTRL or SHIFT)),

        DrawCommand.UNDO.keyCommand to (Hotkey( KeyEvent.VK_Z, CTRL)),
        DrawCommand.REDO.keyCommand to (Hotkey( KeyEvent.VK_Z, CTRL or SHIFT)),
        DrawCommand.CLEAR.keyCommand to (Hotkey( KeyEvent.VK_DELETE, 0)),
        DrawCommand.INVERT.keyCommand to (Hotkey( KeyEvent.VK_I, CTRL )),
        //KeyCommand("draw.toggle_reference") to (Hotkey( KeyEvent.VK_BACK_QUOTE, 0)),
        KeyCommand("draw.lift_to_reference") to (Hotkey( KeyEvent.VK_F, CTRL or SHIFT)),

        DrawCommand.SHIFT_LEFT.keyCommand to (Hotkey( KeyEvent.VK_LEFT, SHIFT or CTRL)),
        DrawCommand.SHIFT_RIGHT.keyCommand to (Hotkey( KeyEvent.VK_RIGHT, SHIFT or CTRL)),
        DrawCommand.SHIFT_DOWN.keyCommand to (Hotkey( KeyEvent.VK_DOWN, SHIFT or CTRL)),
        DrawCommand.SHIFT_UP.keyCommand to (Hotkey( KeyEvent.VK_UP, SHIFT or CTRL)),
        DrawCommand.SCALE3x.keyCommand to (Hotkey( KeyEvent.VK_F3, SHIFT)),


        SelectCommand.All.keyCommand to (Hotkey( KeyEvent.VK_A, CTRL)),
        SelectCommand.None.keyCommand to (Hotkey( KeyEvent.VK_D, CTRL or SHIFT)),
        SelectCommand.Invert.keyCommand to (Hotkey( KeyEvent.VK_I, CTRL or SHIFT)),
        SelectCommand.LiftInPlace.keyCommand to (Hotkey( KeyEvent.VK_X, SHIFT)),

        GlobalCommands.SaveWorkspace.keyCommand to (Hotkey( KeyEvent.VK_S, CTRL)),
        GlobalCommands.Copy.keyCommand to (Hotkey( KeyEvent.VK_C, CTRL)),
        GlobalCommands.CopyVisible.keyCommand to (Hotkey( KeyEvent.VK_C, CTRL or SHIFT)),
        GlobalCommands.Paste.keyCommand to (Hotkey( KeyEvent.VK_V, CTRL)),
        GlobalCommands.PasteAsLayer.keyCommand to (Hotkey( KeyEvent.VK_V, CTRL or SHIFT)),
        GlobalCommands.Cut.keyCommand to (Hotkey( KeyEvent.VK_X, CTRL)),
        GlobalCommands.Open.keyCommand to (Hotkey( KeyEvent.VK_O, CTRL)),
        GlobalCommands.NewWorkspace.keyCommand to (Hotkey( KeyEvent.VK_N, CTRL)),

        // DEBUG
        GlobalCommands.PurgeUndoHistory.keyCommand to (Hotkey(KeyEvent.VK_F11, ALT or SHIFT)),
        GlobalCommands.CopyAllLayer.keyCommand to (Hotkey(KeyEvent.VK_F12, CTRL or SHIFT)),

        // TODO: This should really be animation., but that might require restructuring/rethinking of
        //	command execution system.

        KeyCommand("global.debug1") to (Hotkey( KeyEvent.VK_1, CTRL or SHIFT)),

        KeyCommand("frame.focus.LAYER_PROPERTIES") to (Hotkey(KeyEvent.VK_R, SHIFT)))

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

