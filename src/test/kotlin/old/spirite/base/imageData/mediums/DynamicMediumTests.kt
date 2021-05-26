//package old.spirite.base.imageData.mediums
//
//import io.mockk.every
//import io.mockk.mockk
//import rb.vectrix.linear.MutableTransformF
//import old.TestConfig
//import spirite.base.graphics.DynamicImage
//import spirite.base.imageData.MImageWorkspace
//import spirite.base.imageData.MediumHandle
//import spirite.base.imageData.mediums.ArrangedMediumData
//import spirite.base.imageData.mediums.DynamicMedium
//import rb.glow.Colors
//import sgui.hybrid.EngineLaunchpoint
//import sgui.hybrid.Hybrid
//import rbJvm.glow.awt.AwtImageConverter
//import rbJvm.glow.awt.ImageBI
//import java.io.File
//import javax.imageio.ImageIO
//import kotlin.test.assertEquals
//import org.junit.jupiter.api.Test
//
//class DynamicMediumTests {
//    val mockWorkspace = mockk<MImageWorkspace>(relaxed = true)
//    val imageConverter = AwtImageConverter(EngineLaunchpoint.gle)
//
//    init {
//        every { mockWorkspace.width } returns 100
//        every { mockWorkspace.height } returns 100
//    }
//
//    @Test fun buildsDataAndDrawsToWSCorrectly() {
//
//        val dynamicMedium = DynamicMedium(mockWorkspace, DynamicImage(), mockWorkspace.mediumRepository)
//        val built = dynamicMedium.buildInto(ArrangedMediumData(MediumHandle(mockWorkspace, 0)))
//
//        built.drawOnComposite { gc ->
//            gc.color = Colors.RED
//            gc.fillRect(5,5,10,10)
//        }
//
//        // Save
//        if( TestConfig.save) {
//            val imageBI = imageConverter.convert<ImageBI>(dynamicMedium.image.base!!)
//            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicMedium.png"))
//        }
//
//        // Assert
//        val workspaceImage = Hybrid.imageCreator.createImage(100,100)
//        dynamicMedium.render(workspaceImage.graphics)
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(5,5))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(14,14))
//        assertEquals(0, workspaceImage.getARGB(4,4))
//        assertEquals(0, workspaceImage.getARGB(15,15))
//    }
//
//    @Test fun buildsTransformedDataCorrectly() {
//        val dynamicMedium = DynamicMedium(mockWorkspace, DynamicImage(),mockWorkspace.mediumRepository)
//        val tMediumToWorkspace = MutableTransformF.TranslationMatrix(-10f, -10f)
//        val built = dynamicMedium.buildInto(ArrangedMediumData(MediumHandle(mockWorkspace, 0), tMediumToWorkspace))
//
//        built.drawOnComposite { gc ->
//            // Draw at WS 5,5 = Medium 15,15
//            gc.color = Colors.RED
//            gc.fillRect(5,5,10,10)
//        }
//        built.drawOnComposite { gc ->
//            // Draw at WS 25,25 = Medium 35,35
//            gc.color = Colors.RED
//            gc.fillRect(25,25,10,10)
//        }
//        built.drawOnComposite { gc ->
//            // Draw at WS 55,55 = Medium 65,65
//            gc.color = Colors.RED
//            gc.fillRect(55,55,10,10)
//        }
//
//        // Save
//        val workspaceImage = Hybrid.imageCreator.createImage(100,100)
//        dynamicMedium.render(workspaceImage.graphics)
//
//        if( TestConfig.save) {
//            val imageBI = imageConverter.convert<ImageBI>(workspaceImage)
//            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicMediumTransformed.png"))
//        }
//
//        // Assert
//        assertEquals(0, workspaceImage.getARGB(14,14))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(15,15))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(24,24))
//        assertEquals(0, workspaceImage.getARGB(25,25))
//        assertEquals(0, workspaceImage.getARGB(34,34))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(35,35))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(44,44))
//        assertEquals(0, workspaceImage.getARGB(45,45))
//        assertEquals(0, workspaceImage.getARGB(64,64))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(65,65))
//        assertEquals(Colors.RED.argb, workspaceImage.getARGB(74,74))
//        assertEquals(0, workspaceImage.getARGB(75,75))
//    }
//}