package spirite.base.brains

import spirite.base.brains.Settings.JPreferences
import spirite.base.brains.Settings.SettingsManager
import spirite.base.brains.palette.PaletteManager
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.stroke.GLStrokeDrawerProvider
import spirite.base.graphics.gl.stroke.GLStrokeDrawerV2
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.pen.stroke.StrokeParams
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService

class MasterControl() {
    private val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())
    private val preferences = JPreferences(MasterControl::class.java)

    val frameManager = FrameManager()
    val hotkeyManager = HotkeyManager(preferences)
    val settingsManager = SettingsManager(preferences)

    val paletteManager = PaletteManager()
    val workspaceSet = WorkspaceSet()
    val centralObservatory = CentralObservatory(workspaceSet)

    val strokeDrawerProvider = GLStrokeDrawerProvider(gle)
}