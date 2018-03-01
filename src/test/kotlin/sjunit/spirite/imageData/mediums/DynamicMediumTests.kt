package sjunit.spirite.imageData.mediums

import io.mockk.every
import io.mockk.mockk
import sjunit.TestConfig
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.hybrid.EngineLaunchpoint
import spirite.hybrid.ImageConverter
import spirite.pc.graphics.ImageBI
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Test as test

class DynamicMediumTests {
    val mockWorkspace = mockk<IImageWorkspace>()
    val imageConverter = ImageConverter(EngineLaunchpoint.gle)

    init {
        every { mockWorkspace.width } returns 100
        every { mockWorkspace.height } returns 100
    }

    @test fun buildsDataAndDrawsToWSCorrectly() {
        val dynamicMedium = DynamicMedium(mockWorkspace, DynamicImage())
        val built = dynamicMedium.build(ArrangedMediumData(MediumHandle(mockWorkspace, 0)))

        built.doOnGC { gc ->
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

        built.doOnGC { gc ->
            gc.color = Colors.RED
            gc.fillRect(5,5,10,10)
        }
        built.doOnGC { gc ->
            gc.color = Colors.RED
            gc.fillRect(25,25,10,10)
        }
        built.doOnGC { gc ->
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
}