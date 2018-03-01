package spirite.base.brains

import spirite.base.brains.Settings.JPreferences
import spirite.base.brains.Settings.SettingsManager
import spirite.base.brains.palette.PaletteManager

class MasterControl() {
    private val preferences = JPreferences(MasterControl::class.java)

    val frameManager = FrameManager()
    val hotkeyManager = HotkeyManager(preferences)
    val settingsManager = SettingsManager(preferences)

    val paletteManager = PaletteManager()
    val workspaceSet = WorkspaceSet()
}