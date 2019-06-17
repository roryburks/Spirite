//package sjunit.spirite.base.graphics
//
//
//import rb.vectrix.linear.MutableTransformF
//import sjunit.TestConfig
//import sjunit.TestHelper
//import rb.glow.gle.RenderMethod
//import rb.glow.gle.RenderMethodType.*
//import rb.glow.gle.RenderRubric
//import spirite.base.graphics.rendering.TransformedHandle
//import spirite.base.imageData.MediumHandle
//import rb.glow.color.Colors
//import spirite.hybrid.Hybrid
//import spirite.hybrid.ImageConverter
//import spirite.pc.graphics.ImageBI
//import java.io.File
//import javax.imageio.ImageIO
//import kotlin.test.assertEquals
//import org.junit.Test as test
//
//class TransformedHandleTests {
//    val gle = Hybrid.gle
//    val imageConverter = ImageConverter(gle)
//
//    val workspace = TestHelper.makeShellWorkspace(100,100)
//
//    @test fun testStacks() {
//        val tf = TransformedHandle(
//                MediumHandle(workspace, 0),
//                0,
//                RenderRubric(
//                        MutableTransformF.RotationMatrix(30f),
//                        0.5f,
//                        RenderMethod(COLOR_CHANGE_HUE, Colors.RED.argb)))
//        val rubric2 = RenderRubric(
//                MutableTransformF.TranslationMatrix(-10f,-10f),
//                0.5f,
//                RenderMethod(DISOLVE, Colors.RED.argb))
//
//        val tf3 = tf.stack(rubric2)
//
//        val tExpected = MutableTransformF.RotationMatrix(30f)
//        tExpected.preTranslate(-10f, -10f)
//
//        assertEquals(COLOR_CHANGE_HUE,tf3.renderRubric.methods[0].methodType)
//        assertEquals(DISOLVE,tf3.renderRubric.methods[1].methodType)
//        assertEquals( tExpected, tf3.renderRubric.transform)
//        assertEquals( 0.25f, tf3.renderRubric.alpha)
//    }
//
//    @test fun renders() {
//        val rotate = MutableTransformF.TranslationMatrix(-50f,-50f)
//        rotate.preRotate(1f)
//        rotate.preTranslate(50f,50f)
//        val tf = TransformedHandle(
//                MediumHandle(workspace, 0),
//                0,
//                RenderRubric(
//                        rotate,
//                        0.5f,
//                        RenderMethod(COLOR_CHANGE_FULL, Colors.RED.argb)))
//        val tf2 = RenderRubric(
//                Companion.TranslationMatrix(10f, 10f),
//                0.5f,
//                RenderMethod(DISOLVE, 0b1111_00001111_00001111_00001111))
//
//        val tf3 = tf.stack(tf2)
//
//        val square = Hybrid.imageCreator.createImage(100,100)
//        val squareGC = square.graphics
//        squareGC.color = Colors.GREEN
//        squareGC.fillRect(25,25,50,50)
//
//        val renderedTo = Hybrid.imageCreator.createImage(100,100)
//        val toGC = renderedTo.graphics
//        toGC.renderImage(square, 0, 0, tf3.renderRubric)
//
//        // Assert
//        var reds = 0
//        var trans = 0
//        var other = 0
//        for( x in 25 until 75) {
//            for( y in 25 until 75) {
//                val c = renderedTo.getColor(x,y)
//                when {
//                    c.red == 1f && Math.abs(c.alpha - 0.25f) < 0.01f -> reds++
//                    c.alpha == 0f -> trans++
//                    else -> other++
//                }
//            }
//        }
//        assert( reds > 0)
//        assert( trans > 0)
//        assert( other == 0)
//
//        if( TestConfig.save) {
//            val imageBI = imageConverter.convert<ImageBI>(renderedTo)
//            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\transformedHandleRender.png"))
//        }
//    }
//}