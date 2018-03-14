package spirite.base.brains

import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.Settings.JPreferences
import spirite.base.brains.Settings.SettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteManager
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.toolset.ToolsetManager
import spirite.base.graphics.IResourceUseTracker
import spirite.base.graphics.ResourceUseTracker
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.stroke.GLStrokeDrawerProvider
import spirite.base.graphics.gl.stroke.GLStrokeDrawerV2
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.graphics.rendering.RenderEngine
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.pen.stroke.StrokeParams
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService

/** MasterControl is a top-level container for all the global-level components.  From a dependency-injection perspective
 * you can think of it as the primary provider.
 *
 * For testing purposes all internal components get passed the subcomponents they need (in practice they will all be
 * getting them from MasterControl), but UI components will just be passed MasterControl semi-directly (in the off-chance
 * that you really wanted to selectively test UI components, you can mock IMasterControl instead of using a real one). */
interface IMasterControl {
    val frameManager: IFrameManager
    val hotkeyManager: IHotkeyManager
    val settingsManager : ISettingsManager
    val paletteManager : IPaletteManager
    val workspaceSet : MWorkspaceSet
    val centralObservatory: ICentralObservatory
    val strokeDrawerProvider: IStrokeDrawerProvider
    val toolsetManager: IToolsetManager
    val renderEngine: IRenderEngine
    val resourceUseTracker : IResourceUseTracker

}

class MasterControl() : IMasterControl {
    private val gle = Hybrid.gle
    private val preferences = JPreferences(MasterControl::class.java)

    override val frameManager = FrameManager()
    override val hotkeyManager = HotkeyManager(preferences)
    override val settingsManager = SettingsManager(preferences)

    override val paletteManager = PaletteManager()
    override val workspaceSet = WorkspaceSet()
    override val centralObservatory = CentralObservatory(workspaceSet)

    override val strokeDrawerProvider = GLStrokeDrawerProvider(gle)
    override val toolsetManager = ToolsetManager()
    override val resourceUseTracker = ResourceUseTracker()
    override val renderEngine = RenderEngine(resourceUseTracker, centralObservatory)
}