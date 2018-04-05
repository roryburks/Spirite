package spirite.base.brains

import spirite.base.brains.commands.CentralCommandExecutor
import spirite.base.brains.commands.ICentralCommandExecutor
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.settings.JPreferences
import spirite.base.brains.settings.SettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteManager
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.toolset.ToolsetManager
import spirite.base.file.FileManager
import spirite.base.file.IFileManager
import spirite.base.graphics.IResourceUseTracker
import spirite.base.graphics.ResourceUseTracker
import spirite.base.graphics.gl.stroke.GLStrokeDrawerProvider
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.graphics.rendering.RenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.gui.components.dialogs.JDialog
import spirite.gui.components.dialogs.IDialog
import spirite.gui.menus.ContextMenus
import spirite.hybrid.Hybrid
import spirite.pc.gui.menus.SwContextMenus

/** MasterControl is a top-level container for all the global-level components.  From a dependency-injection perspective
 * you can think of it as the primary provider.
 *
 * For testing purposes all internal components get passed the subcomponents they need (in practice they will all be
 * getting them from MasterControl), but UI components will just be passed MasterControl semi-directly (in the off-chance
 * that you really wanted to selectively test UI components, you can mock IMasterControl instead of using a real one). */
interface IMasterControl {
    val hotkeyManager: IHotkeyManager
    val settingsManager : ISettingsManager
    val paletteManager : IPaletteManager
    val workspaceSet : MWorkspaceSet
    val centralObservatory: ICentralObservatory
    val strokeDrawerProvider: IStrokeDrawerProvider
    val toolsetManager: IToolsetManager
    val renderEngine: IRenderEngine
    val resourceUseTracker : IResourceUseTracker
    val commandExecuter : ICentralCommandExecutor
    val fileManager: IFileManager

    val frameManager: IFrameManager
    val contextMenus : ContextMenus
    val dialog : IDialog

    fun createWorkspace(width: Int, height: Int) : MImageWorkspace
}

class MasterControl() : IMasterControl {

    private val gle = Hybrid.gle
    private val preferences = JPreferences(MasterControl::class.java)
    override val dialog: IDialog = JDialog(this)

    override val hotkeyManager = HotkeyManager(preferences)
    override val settingsManager = SettingsManager(preferences)

    override val paletteManager = PaletteManager()
    override val workspaceSet = WorkspaceSet()
    override val centralObservatory = CentralObservatory(workspaceSet)

    override val strokeDrawerProvider = GLStrokeDrawerProvider(gle)
    override val toolsetManager = ToolsetManager()
    override val resourceUseTracker = ResourceUseTracker()
    override val renderEngine = RenderEngine(resourceUseTracker, centralObservatory)
    override val commandExecuter = CentralCommandExecutor(this, workspaceSet, dialog)

    override val frameManager = FrameManager()
    override val contextMenus: ContextMenus = SwContextMenus(commandExecuter)
    override val fileManager = FileManager(this)


    override fun createWorkspace(width: Int, height: Int) = ImageWorkspace(
            renderEngine,
            settingsManager,
            paletteManager,
            strokeDrawerProvider,
            width,
            height)
}