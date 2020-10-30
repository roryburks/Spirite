package spirite.base.brains

import rb.glow.img.IImage
import rbJvm.glow.awt.NativeImage
import sguiSwing.hybrid.Hybrid
import spirite.base.brains.commands.CentralCommandExecutor
import spirite.base.brains.commands.ICentralCommandExecutor
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.palette.PaletteManager
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.settings.JPreferences
import spirite.base.brains.settings.SettingsManager
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.toolset.ToolsetManager
import spirite.base.file.FileManager
import spirite.base.file.IFileManager
import spirite.base.graphics.DetailedResourceUseTracker
import spirite.base.graphics.IDetailedResourceUseTracker
import spirite.base.graphics.rendering.*
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.gui.implementations.topLevelFeedback.SwTopLevelFeedbackSystem
import spirite.gui.menus.IContextMenus
import spirite.gui.menus.dialogs.IDialog
import spirite.gui.menus.dialogs.JDialog
import spirite.pc.menus.SwContextMenus
import spirite.specialRendering.stroke.GLStrokeDrawerProvider

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
    val resourceUseTracker : IDetailedResourceUseTracker
    val commandExecutor : ICentralCommandExecutor
    val fileManager: IFileManager

    val frameManager: IFrameManager
    val contextMenus : IContextMenus
    val dialog : IDialog
    val topLevelFeedbackSystem : ITopLevelFeedbackSystem

    val thumbnailStore: IThumbnailStore<IImage>
    val nativeThumbnailStore : IThumbnailStore<NativeImage>

    fun createWorkspace(width: Int, height: Int) : MImageWorkspace
}

class MasterControl : IMasterControl {
    // Unfortunately this is a bit of a cluster of order-dependence.  No proper DI library.

    private val gle = Hybrid.gle
    private val preferences = JPreferences(MasterControl::class.java)
    override val dialog: IDialog = JDialog(this)
    override val topLevelFeedbackSystem = SwTopLevelFeedbackSystem()

    override val hotkeyManager = HotkeyManager(preferences)
    override val settingsManager = SettingsManager(preferences)

    override val workspaceSet = WorkspaceSet()
    override val centralObservatory = CentralObservatory(workspaceSet)
    override val paletteManager = PaletteManager(workspaceSet, settingsManager, dialog, centralObservatory)

    override val strokeDrawerProvider = GLStrokeDrawerProvider(gle)
    override val toolsetManager = ToolsetManager()
    override val resourceUseTracker = DetailedResourceUseTracker()
    override val renderEngine = RenderEngine(resourceUseTracker, centralObservatory)

    override val frameManager = SwFrameManager(this)
    override val fileManager = FileManager(this)


    override val commandExecutor = CentralCommandExecutor(this, workspaceSet, dialog)

    override val contextMenus: IContextMenus = SwContextMenus(commandExecutor)

    override val thumbnailStore: IThumbnailStore<IImage>
    override val nativeThumbnailStore: IThumbnailStore<NativeImage>


    init {
        val _thumbnailStore = ThumbnailStore(settingsManager, centralObservatory,workspaceSet)
        thumbnailStore = _thumbnailStore
        nativeThumbnailStore = DerivedNativeThumbnailStore(_thumbnailStore)
        topLevelFeedbackSystem.frameManager = frameManager
    }

    override fun createWorkspace(width: Int, height: Int) = ImageWorkspace(
            renderEngine,
            settingsManager,
            paletteManager,
            strokeDrawerProvider,
            toolsetManager.toolset,
            width,
            height)
}