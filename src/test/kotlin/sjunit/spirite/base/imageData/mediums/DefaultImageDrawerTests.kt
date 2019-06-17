package sjunit.spirite.base.imageData.mediums

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import rb.glow.color.Colors
import sjunit.TestConfig
import sjunit.TestHelper
import spirite.specialRendering.stroke.GLStrokeDrawerV2
import spirite.base.graphics.rendering.NodeRenderer
import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.MediumType.DYNAMIC
import spirite.base.imageData.mediums.MediumType.FLAT
import spirite.base.pen.PenState
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.pen.stroke.StrokeParams
import spirite.hybrid.Hybrid
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DefaultImageDrawerTests {
    private val mockStrokeProvider = mockk<IStrokeDrawerProvider>()
    private val workspace = TestHelper.makeShellWorkspace( strokeProvider = mockStrokeProvider)

    @Test fun compositeDraw() {
        // Arrange
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "layer1", FLAT, 25,25)
        layer1.x = 20
        layer1.y = 20

        every { mockStrokeProvider.getStrokeDrawer(any()) }.returns(GLStrokeDrawerV2(Hybrid.gle))

        val drawer = layer1.layer.getDrawer(workspace.arrangeActiveDataForNode(layer1)) as IStrokeModule

        // Act
        drawer.startStroke(StrokeParams(color = Colors.BLUE, width = 4f), PenState(0f, 0f, 0.5f))
        drawer.stepStroke(PenState(40f, 10f, 1f))
        drawer.stepStroke(PenState(45f, 20f, 0.75f))
        drawer.stepStroke(PenState(20f, 45f, 0.25f))

        val wsImage = Hybrid.imageCreator.createImage(100,100)
        NodeRenderer(workspace.groupTree.root, workspace).render(wsImage.graphics)

        // Save
        TestConfig.trySave(wsImage,"defaultImageDrawer_compositeStroke")

        // Assert
        assertNotEquals( 0, wsImage.getARGB(44, 20))
        assertNotEquals( 0, wsImage.getARGB(20, 44))
        assertEquals( 0, wsImage.getARGB(40, 10))
    }

    @Test fun draws() {
        // Arrange
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "layer1", FLAT, 25,25)
        layer1.x = 20
        layer1.y = 20

        every { mockStrokeProvider.getStrokeDrawer(any()) }.returns(GLStrokeDrawerV2(Hybrid.gle))

        val drawer = layer1.layer.getDrawer(workspace.arrangeActiveDataForNode(layer1)) as IStrokeModule

        // Act
        drawer.startStroke(StrokeParams(color = Colors.BLUE, width = 4f), PenState(0f, 0f, 0.5f))
        drawer.stepStroke(PenState(40f, 10f, 1f))
        drawer.stepStroke(PenState(45f, 20f, 0.75f))
        drawer.stepStroke(PenState(20f, 45f, 0.25f))
        drawer.endStroke()

        // Save
        val image = ((layer1.layer as SimpleLayer).medium.medium as FlatMedium).image
        TestConfig.trySave( image, "defaultImageDrawer_draws")

        // Assert
        assertNotEquals( 0, image.getARGB(24, 0))
        assertNotEquals( 0, image.getARGB(0, 24))
        assertEquals( 0, image.getARGB(0, 0))


    }

    @Test fun undoesAndRedoes() {
        // Arrange
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "layer1", FLAT, 25,25)
        layer1.x = 20
        layer1.y = 20

        every { mockStrokeProvider.getStrokeDrawer(any()) }.returns(GLStrokeDrawerV2(Hybrid.gle))

        val drawer = layer1.layer.getDrawer(workspace.arrangeActiveDataForNode(layer1)) as IStrokeModule

        // Act
        drawer.startStroke(StrokeParams(color = Colors.BLUE, width = 4f), PenState(0f, 0f, 0.5f))
        drawer.stepStroke(PenState(40f, 10f, 1f))
        drawer.stepStroke(PenState(45f, 20f, 0.75f))
        drawer.stepStroke(PenState(20f, 45f, 0.25f))
        drawer.endStroke()
        workspace.undoEngine.undo()

        // Save

        // Assert + More Act
        var image = ((layer1.layer as SimpleLayer).medium.medium as FlatMedium).image
        (0 until image.width).forEach { x -> (0 until image.height).forEach{ y -> assertEquals(0, image.getARGB(x,y))} }

        workspace.undoEngine.redo()
        image = ((layer1.layer as SimpleLayer).medium.medium as FlatMedium).image
        TestConfig.trySave(image, "defaultImageDrawer_redo")

        assertNotEquals( 0, image.getARGB(24, 0))
        assertNotEquals( 0, image.getARGB(0, 24))
        assertEquals( 0, image.getARGB(0, 0))
    }

    @Test fun drawsOnDynamic() {
        // Arrange
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "layer1", DYNAMIC, 25,25)
        layer1.x = 20
        layer1.y = 20

        every { mockStrokeProvider.getStrokeDrawer(any()) }.returns(GLStrokeDrawerV2(Hybrid.gle))

        val drawer = layer1.layer.getDrawer(workspace.arrangeActiveDataForNode(layer1)) as IStrokeModule

        // Act
        drawer.startStroke(StrokeParams(color = Colors.BLUE, width = 4f), PenState(10f, 10f, 0.5f))
        drawer.stepStroke(PenState(30f, 12f, 0.5f))
        drawer.stepStroke(PenState(50f, 20f, 0.5f))
        drawer.stepStroke(PenState(55f, 30f, 0.75f))
        drawer.stepStroke(PenState(30f, 55f, 0.25f))
        drawer.endStroke()

        // Save
        val image = ((layer1.layer as SimpleLayer).medium.medium as DynamicMedium).image.base!!
        TestConfig.trySave(image, "defaultImageDrawer_dynamic")
    }

    @Test fun compositesCorrectlyOnDynamic() {
        // Arrange
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "layer1", DYNAMIC, 25,25)
        layer1.x = 20
        layer1.y = 20

        workspace.width = 300
        workspace.height = 300

        every { mockStrokeProvider.getStrokeDrawer(any()) }.returns(GLStrokeDrawerV2(Hybrid.gle))

        val drawer = layer1.layer.getDrawer(workspace.arrangeActiveDataForNode(layer1)) as IStrokeModule

        // Act
        drawer.startStroke(StrokeParams(color = Colors.BLUE, width = 4f), PenState(10f, 10f, 0.5f))
        drawer.stepStroke(PenState(30f, 12f, 0.5f))
        drawer.stepStroke(PenState(50f, 20f, 0.5f))
        drawer.stepStroke(PenState(55f, 30f, 0.75f))
        drawer.stepStroke(PenState(30f, 55f, 0.25f))
        drawer.endStroke()

        drawer.startStroke(StrokeParams(color = Colors.RED, width = 4f), PenState(70f, 50f, 0.5f))
        drawer.stepStroke(PenState(60f, 60f, 0.5f))
        drawer.stepStroke(PenState(50f, 70f, 0.5f))

        // Save
        val wsImage = Hybrid.imageCreator.createImage(workspace.width,workspace.height)
        NodeRenderer(workspace.groupTree.root, workspace).render(wsImage.graphics)
        TestConfig.trySave(wsImage, "defaultImageDrawer_dynamicComposite")
    }
}