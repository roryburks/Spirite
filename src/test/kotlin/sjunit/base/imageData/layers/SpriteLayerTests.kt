package sjunit.base.imageData.layers

import io.mockk.every
import io.mockk.mockk
import sjunit.testHelpers.PassthroughUndoEngine
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.layers.sprite.SpriteLayer
import kotlin.test.assertTrue
import org.junit.Test as test

class SpriteLayerTests{
    class MockUndo {
        val _mockWorkspace = mockk<MImageWorkspace>(relaxed = true)
        val _layer = SpriteLayer(_mockWorkspace)
        init {
            every { _mockWorkspace.undoEngine }.returns(PassthroughUndoEngine)
        }

        @test fun basicAddLayers() {
            _layer.addPart("layer2")
            _layer.addPart("layer3")

            val partNames = _layer.parts.map { it.partName }

            assertTrue { partNames.contains("base") }
            assertTrue { partNames.contains("layer2") }
            assertTrue { partNames.contains("layer3") }
        }

        @test fun addChangeLayersNameToDupe(){
            _layer.addPart("layer2")
            _layer.addPart("layer3")

            _layer.parts .first { it.partName == "layer2" } .partName = "layer0"
            _layer.parts .first { it.partName == "layer3" } .partName = "base"

            val partNames = _layer.parts.map { it.partName }

            assertTrue { partNames.contains("base") }
            assertTrue { partNames.contains("layer0") }
            assertTrue { partNames.contains("base_0") }
        }
    }
}
