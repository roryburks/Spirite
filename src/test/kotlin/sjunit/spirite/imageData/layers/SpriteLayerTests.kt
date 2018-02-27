package sjunit.spirite.imageData.layers


import io.mockk.mockk
import spirite.base.brains.Settings.IPreferences
import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.graphics.DynamicImage
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.DynamicMedium
import kotlin.test.assertEquals
import org.junit.Test as test

class SpriteLayerTests {
    val renderEngine = mockk<IRenderEngine>()
    val settingsManager = mockk<ISettingsManager>()
    val paletteManager = mockk<IPaletteManager>()
    val workspace = ImageWorkspace(renderEngine, settingsManager, paletteManager)

    @test fun makesSpriteLayer() {
        val spriteLayer = SpriteLayer(workspace, workspace.mediumRepository)
        (spriteLayer.parts[0].handle.medium as DynamicMedium).image.drawToImage({
            it.graphics.drawLine(0f,0f,10f,10f)
        }, 100, 100)

        assertEquals(10, spriteLayer.width)
        assertEquals(10, spriteLayer.height)
    }

    @test fun addsLayer() {
        val spriteLayer = SpriteLayer(workspace, workspace.mediumRepository)
        spriteLayer.addPart("foot")

        assertEquals(2, spriteLayer.parts.size)
        assertEquals( 0, spriteLayer.parts[0].depth)
        assertEquals( 1, spriteLayer.parts[1].depth)

        workspace.undoEngine.undo()
        assertEquals(1, spriteLayer.parts.size)
        assertEquals( 0, spriteLayer.parts[0].depth)

        workspace.undoEngine.redo()
        assertEquals(2, spriteLayer.parts.size)
        assertEquals( 0, spriteLayer.parts[0].depth)
        assertEquals( 1, spriteLayer.parts[1].depth)
    }

    @test fun editsProperties() {
        val layer1 = SpriteLayer(workspace, workspace.mediumRepository)
        val layer2 = SpriteLayer(workspace, workspace.mediumRepository)

        layer1.parts[0].alpha = 0.5f
        layer1.parts[0].alpha = 0.25f
        layer1.parts[0].scaleX = 0.33f

        assertEquals(1, workspace.undoEngine.undoHistory.count())
        assertEquals(0.25f, layer1.parts[0].alpha)
        assertEquals(0.33f, layer1.parts[0].scaleX)

        workspace.undoEngine.undo()
        assertEquals(1f, layer1.parts[0].alpha)
        assertEquals(1f, layer1.parts[0].scaleX)
        workspace.undoEngine.redo()
        assertEquals(0.25f, layer1.parts[0].alpha)
        assertEquals(0.33f, layer1.parts[0].scaleX)

        layer2.parts[0].alpha = 0.5f
        assertEquals(2, workspace.undoEngine.undoHistory.count())
    }

    @test fun sortsByDepth() {
        // Todo
    }
}