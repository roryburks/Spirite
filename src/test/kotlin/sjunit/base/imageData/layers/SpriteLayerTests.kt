package sjunit.base.imageData.layers

import io.mockk.every
import io.mockk.mockk
import sgui.components.IButton
import sjunit.testHelpers.MockedWorkspaceSet
import sjunit.testHelpers.PassthroughUndoEngine
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.gui.views.layerProperties.SpriteLayerPanel
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

    class PartManagementTests{
        val mockSet = MockedWorkspaceSet()
        val ws get() = mockSet.ws

        @test fun tryToDuplicateWrongDeleteBug(){
            val layer = SpriteLayer(ws)
            layer.activePart = layer.parts.firstOrNull()
            layer.activePart?.partName = "layer1"

            layer.addPart("layer2")
            layer.addPart("layer3")
            layer.addPart("layer4")
            layer.addPart("layer5")

            layer.activePart = layer.parts.firstOrNull { it.partName == "layer3" }

            layer.activePart?.apply { layer.removePart(this)}

            print(layer.parts.map { it.partName })

            assertTrue { layer.parts.any { it.partName == "layer1" } }
            assertTrue { layer.parts.any { it.partName == "layer2" } }
            assertTrue {!layer.parts.any { it.partName == "layer3" } }
            assertTrue { layer.parts.any { it.partName == "layer4" } }
            assertTrue { layer.parts.any { it.partName == "layer5" } }
        }

        @test fun wrongSelectedPartBug_Task118(){
            val panel = SpriteLayerPanel(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            val layer = SpriteLayer(ws)
            panel.linkedSprite = layer

            layer.addPart("layer2")
            layer.addPart("layer3")

            panel.boxList.data.selected = layer.parts.firstOrNull { it.partName == "layer2" }

            panel.btnRemovePart.action?.invoke(IButton.ButtonActionEvent(true, false, false))

            assertTrue { layer.parts.any { it.partName == "base" } }
            assertTrue { !layer.parts.any { it.partName == "layer2" } }
            assertTrue { layer.parts.any { it.partName == "layer3" } }
        }
    }
}
