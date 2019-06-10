//package sjunit.spirite.base.imageData.mediums
//
//import io.mockk.mockk
//import rb.vectrix.linear.MutableTransformF
//import sjunit.TestConfig
//import spirite.base.imageData.MImageWorkspace
//import spirite.base.imageData.MediumHandle
//import spirite.base.imageData.mediums.ArrangedMediumData
//import spirite.base.imageData.mediums.FlatMedium
//import sgui.generic.color.Colors
//import spirite.hybrid.EngineLaunchpoint
//import spirite.hybrid.Hybrid
//import spirite.hybrid.ImageConverter
//import spirite.pc.graphics.ImageBI
//import java.io.File
//import javax.imageio.ImageIO
//import kotlin.test.assertEquals
//import org.junit.Test as test
//
//class FlatMediumTests {
//    val mockWorkspace = mockk<MImageWorkspace>(relaxed = true)
//    val imageConverter = ImageConverter(EngineLaunchpoint.gle)
//
//    @test fun buildsDataCorrectly() {
//        val flatMedium = FlatMedium( Hybrid.imageCreator.createImage( 20, 20), mockWorkspace.mediumRepository)
//        val built = flatMedium.buildInto(ArrangedMediumData(MediumHandle(mockWorkspace, 0)))
//
//        built.drawOnComposite { gc ->
//            gc.color = Colors.RED
//            gc.fillRect(5,5,10,10)
//        }
//
//        assertEquals(0xffff0000.toInt(), flatMedium.image.getColor(5,5).argb32)
//        assertEquals(0xffff0000.toInt(), flatMedium.image.getColor(14,14).argb32)
//        assertEquals(0, flatMedium.image.getColor(4,4).argb32)
//        assertEquals(0, flatMedium.image.getColor(15,15).argb32)
//
//        if( TestConfig.save) {
//            val imageBI = imageConverter.convert<ImageBI>(flatMedium.image)
//            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\flatMedium.png"))
//        }
//    }
//
//    @test fun buildsTransformedDataCorrectly() {
//        val flatMedium = FlatMedium( Hybrid.imageCreator.createImage( 20, 20),mockWorkspace.mediumRepository)
//        val transform = MutableTransformF.TranslationMatrix(-10f,-10f)
//
//        // Note: because we are using a non-standard coordinate setup (yi down) and we aren't using helper functions that
//        //  make rotation behave in a more intuitive way for the coordinate system, positive rotation rotates clockwise
//        //  rather than counterclockwise
//        transform.preRotate(-0.785398163f)
//        transform.preTranslate(30f,30f)
//        val built = flatMedium.buildInto(ArrangedMediumData(MediumHandle(mockWorkspace, 0), transform))
//
//        built.drawOnComposite { gc ->
//            gc.color = Colors.RED
//            gc.fillRect(0,0,30,30)
//        }
//
//        assertEquals(0xffff0000.toInt(), flatMedium.image.getColor(18,0).argb32)
//        assertEquals(0xffff0000.toInt(), flatMedium.image.getColor(9,8).argb32)
//        assertEquals(0, flatMedium.image.getColor(0,9).argb32)
//        assertEquals(0, flatMedium.image.getColor(19,9).argb32)
//
//        if( TestConfig.save) {
//            val imageBI = imageConverter.convert<ImageBI>(flatMedium.image)
//            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\flatMediumTransformed.png"))
//        }
//    }
//    @test fun buildsTransformedDataCorrectlyThenDisplaysCorrectly() {
//        val workspaceImage = Hybrid.imageCreator.createImage( 50, 50)
//        val flatMedium = FlatMedium( Hybrid.imageCreator.createImage( 20, 20),mockWorkspace.mediumRepository)
//        val transform = MutableTransformF.TranslationMatrix(-10f,-10f)
//
//        // Note: because we are using a non-standard coordinate setup (yi down) and we aren't using helper functions that
//        //  make rotation behave in a more intuitive way for the coordinate system, positive rotation rotates clockwise
//        //  rather than counterclockwise
//        transform.preRotate(-0.785398163f)
//        transform.preTranslate(30f,30f)
//        val built = flatMedium.buildInto(ArrangedMediumData(MediumHandle(mockWorkspace, 0), transform))
//
//        built.drawOnComposite { gc ->
//            gc.color = Colors.RED
//            gc.fillRect(0,0,30,30)
//        }
//
//        val gc = workspaceImage.graphics
//        gc.transform = transform
//        flatMedium.render(gc)
//
//        if( TestConfig.save) {
//            val imageBI = imageConverter.convert<ImageBI>(workspaceImage)
//            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\flatMediumTransformedToWorkspace.png"))
//        }
//    }
//}