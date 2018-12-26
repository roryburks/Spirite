package sjunit

import io.mockk.mockk
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.pen.stroke.IStrokeDrawerProvider

object TestHelper {
    val mockRenderEngine=  mockk<IRenderEngine>(relaxed = true)
    val mockSettingsManager = mockk<ISettingsManager>(relaxed = true)
    val mockPaletteManager = mockk<IPaletteManager>(relaxed = true)
    val mockStrokeProvider = mockk<IStrokeDrawerProvider>(relaxed = true)
    val mockToolset = mockk<IToolsetManager>(relaxed = true)

    /** Creates a Workspace with all the external modules mocked with a relaxed Mocker */
    fun makeShellWorkspace( w: Int = 100, h: Int = 100,
                            renderEngine : IRenderEngine = mockRenderEngine,
                            settingsManager : ISettingsManager = mockSettingsManager,
                            paletteManager: IPaletteManager = mockPaletteManager,
                            strokeProvider: IStrokeDrawerProvider = mockStrokeProvider) : ImageWorkspace
    {
        return ImageWorkspace(
                renderEngine,
                settingsManager,
                paletteManager,
                strokeProvider,
                mockToolset.toolset,
                w, h)
    }
}