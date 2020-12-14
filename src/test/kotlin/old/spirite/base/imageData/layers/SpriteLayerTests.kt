package old.spirite.base.imageData.layers


import old.TestHelper
import org.junit.jupiter.api.Test
import rb.glow.drawer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.DynamicMedium
import kotlin.test.assertEquals

class SpriteLayerTests {
    val workspace = TestHelper.makeShellWorkspace(100,100)

    @Test fun makesSpriteLayer() {
        val spriteLayer = SpriteLayer(workspace)
        (spriteLayer.parts[0].handle.medium as DynamicMedium).image.drawToImage(100, 100, drawer = {
            it.graphics.drawer.drawLine(0.0,0.0,10.0,10.0)
        })

        assertEquals(10, spriteLayer.width)
        assertEquals(10, spriteLayer.height)
    }

    @Test fun addsLayer() {
        val spriteLayer = SpriteLayer(workspace)
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

    @Test fun editsProperties() {
        val layer1 = SpriteLayer(workspace)
        val layer2 = SpriteLayer(workspace)

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

    @Test fun sortsByDepth() {
        // Todo
    }
}