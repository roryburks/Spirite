package sjunit

import io.mockk.mockk
import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.ImageWorkspace

object TestHelper {
    val mockRenderEngine=  mockk<IRenderEngine>(relaxed = true)
    val mockSettingsManager = mockk<ISettingsManager>(relaxed = true)
    val mockPaletteManager = mockk<IPaletteManager>(relaxed = true)

    fun makeShellWorkspace( w: Int = 100, h: Int = 100) = ImageWorkspace(mockRenderEngine, mockSettingsManager, mockPaletteManager, w, h)
}