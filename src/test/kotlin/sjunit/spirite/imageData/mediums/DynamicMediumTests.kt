package sjunit.spirite.imageData.mediums

import io.mockk.every
import io.mockk.mockk
import sjunit.TestConfig
import sjunit.TestHelper
import sjunit.spirite.imageData.groupTree.PrimaryGroupTreeTests
import spirite.base.graphics.DynamicImage
import spirite.base.graphics.rendering.NodeRenderer
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.PrimaryGroupTree
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium.MediumType.DYNAMIC
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.Hybrid
import spirite.hybrid.ImageConverter
import spirite.pc.graphics.ImageBI
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Test as test

class DynamicMediumTests {
    val mockWorkspace = mockk<IImageWorkspace>()
    val workspace = TestHelper.makeShellWorkspace(100,100)
    val imageConverter = ImageConverter(EngineLaunchpoint.gle)

    init {
        every { mockWorkspace.width } returns 100
        every { mockWorkspace.height } returns 100
    }

    @test fun buildsDataAndDrawsToWSCorrectly() {
        val dynamicMedium = DynamicMedium(mockWorkspace, DynamicImage())
        val built = dynamicMedium.build(ArrangedMediumData(MediumHandle(mockWorkspace, 0)))

        built.drawOnComposite { gc ->
            gc.color = Colors.RED
            gc.fillRect(5,5,10,10)
        }

        val workspaceImage = EngineLaunchpoint.createImage(100,100)
        dynamicMedium.render(workspaceImage.graphics)
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(5,5))
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(14,14))
        assertEquals(0, workspaceImage.getARGB(4,4))
        assertEquals(0, workspaceImage.getARGB(15,15))

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(dynamicMedium.image.base!!)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicMedium.png"))
        }
    }

    @test fun buildsTransformedDataCorrectly() {
        val dynamicMedium = DynamicMedium(mockWorkspace, DynamicImage())
        val tMediumToWorkspace = MutableTransform.TranslationMatrix(-10f, -10f)
        val built = dynamicMedium.build(ArrangedMediumData(MediumHandle(mockWorkspace, 0), tMediumToWorkspace))

        built.drawOnComposite { gc ->
            gc.color = Colors.RED
            gc.fillRect(5,5,10,10)
        }
        built.drawOnComposite { gc ->
            gc.color = Colors.RED
            gc.fillRect(25,25,10,10)
        }
        built.drawOnComposite { gc ->
            gc.color = Colors.RED
            gc.fillRect(55,55,10,10)
        }

        val workspaceImage = EngineLaunchpoint.createImage(100,100)
        dynamicMedium.render(workspaceImage.graphics)
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(0,0))
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(4,4))
        assertEquals(0, workspaceImage.getARGB(5,5))
        assertEquals(0, workspaceImage.getARGB(14,14))
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(15,15))
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(24,24))
        assertEquals(0, workspaceImage.getARGB(25,25))
        assertEquals(0, workspaceImage.getARGB(44,44))
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(45,45))
        assertEquals(Colors.RED.argb, workspaceImage.getARGB(54,54))
        assertEquals(0, workspaceImage.getARGB(55,55))

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(dynamicMedium.image.base!!)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicMediumTransformed.png"))
        }
    }

    @test fun compositesCorrectly() {
        val layer1 = workspace.groupTree.addNewSimpleLayer(null, "Layer1", DYNAMIC)
        layer1.x = 10
        layer1.y = 10

        val mediumHandle = (layer1.layer as SimpleLayer).medium
        val dynamicImage = (mediumHandle.medium as DynamicMedium).image

        dynamicImage.drawToImage({
            it.graphics.fillRect(0,0,20,20)
        }, 20, 20)


        val tMediumToWS = MutableTransform.TranslationMatrix(10f,10f)
        workspace.compositor.compositeSource = CompositeSource(
                ArrangedMediumData(mediumHandle, tMediumToWS),
                {it.color = Colors.RED
                    it.fillRect( 40,40, 10, 10)})


        val wsImage = Hybrid.imageCreator.createImage(100,100)

        NodeRenderer(workspace.groupTree.root, workspace).render(wsImage.graphics)


        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(wsImage)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicComposited.png"))
        }

        for( x in 10 until 30)
            for( y in 10 until 30)
                assertEquals(Colors.BLACK.argb, wsImage.getARGB(x, y))
        for( x in 40 until 50)
            for( y in 40 until 50)
                assertEquals(Colors.RED.argb,wsImage.getARGB(x,y))
    }
}