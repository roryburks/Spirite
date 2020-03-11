package sjunit.testHelpers

import io.mockk.mockk
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.settings.ISettingsManager
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.toolset.Toolset
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.pen.stroke.IStrokeDrawerProvider

fun  makeWorkspaceWithMockedExternals(width: Int = 100, height: Int = 100) =  ImageWorkspace(
            mockk<IRenderEngine>(relaxed = true),
            mockk<ISettingsManager>(relaxed = true),
            mockk<IPaletteManager>(relaxed=true),
            mockk<IStrokeDrawerProvider>(relaxed=true),
            Toolset(mockk<IToolsetManager>(relaxed = true)),
            width, height )

class MockedWorkspaceSet(width: Int = 100, height: Int = 100) {
    val mockRenderEngine = mockk<IRenderEngine>(relaxed = true)
    val mockSettingsManager = mockk<ISettingsManager>(relaxed = true)
    val mockPaletteManager = mockk<IPaletteManager>(relaxed = true)
    val mockStrokeDrawerProvider = mockk<IStrokeDrawerProvider>(relaxed = true)
    val mockToolsetManager = mockk<IToolsetManager>(relaxed = true)
    val ws = ImageWorkspace(
            mockRenderEngine,
            mockSettingsManager,
            mockPaletteManager,
            mockStrokeDrawerProvider,
            Toolset(mockToolsetManager))
}
