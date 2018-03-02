package sjunit.spirite.graphics


import io.mockk.mockk
import sjunit.TestConfig
import sjunit.TestHelper
import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.graphics.RenderMethod
import spirite.base.graphics.RenderMethodType.*
import spirite.base.graphics.RenderRubric
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Transform.Companion
import spirite.hybrid.Hybrid
import spirite.hybrid.ImageConverter
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.graphics.ImageBI
import spirite.pc.resources.JClassScriptService
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Test as test

class TransformedHandleTests {
    val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())
    val imageConverter = ImageConverter(gle)

    val workspace = TestHelper.makeShellWorkspace(100,100)

    @test fun testStacks() {
        val tf = TransformedHandle(
                MediumHandle(workspace, 0),
                0,
                RenderRubric(
                        MutableTransform.RotationMatrix(30f),
                        0.5f,
                        RenderMethod(COLOR_CHANGE_HUE, Colors.RED.argb)))
        val rubric2 = RenderRubric(
                MutableTransform.TranslationMatrix(-10f,-10f),
                0.5f,
                RenderMethod(DISOLVE, Colors.RED.argb))

        val tf3 = tf.stack(rubric2)

        val tExpected = MutableTransform.RotationMatrix(30f)
        tExpected.preTranslate(-10f, -10f)

        assertEquals(COLOR_CHANGE_HUE,tf3.renderRubric.methods[0].methodType)
        assertEquals(DISOLVE,tf3.renderRubric.methods[1].methodType)
        assertEquals( tExpected, tf3.renderRubric.transform)
        assertEquals( 0.25f, tf3.renderRubric.alpha)
    }

    @test fun renders() {
        val rotate = MutableTransform.TranslationMatrix(-50f,-50f)
        rotate.preRotate(1f)
        rotate.preTranslate(50f,50f)
        val tf = TransformedHandle(
                MediumHandle(workspace, 0),
                0,
                RenderRubric(
                        rotate,
                        0.5f,
                        RenderMethod(COLOR_CHANGE_FULL, Colors.RED.argb)))
        val tf2 = RenderRubric(
                Companion.TranslationMatrix(10f, 10f),
                0.5f,
                RenderMethod(DISOLVE, 0b1111_00001111_00001111_00001111))

        val tf3 = tf.stack(tf2)

        val square = Hybrid.imageCreator.createImage(100,100)
        val squareGC = square.graphics
        squareGC.color = Colors.GREEN
        squareGC.fillRect(25,25,50,50)

        val renderedTo = Hybrid.imageCreator.createImage(100,100)
        val toGC = renderedTo.graphics
        toGC.renderImage(square, 0, 0, tf3.renderRubric)

        // Assert
        var reds = 0
        var trans = 0
        var other = 0
        for( x in 25 until 75) {
            for( y in 25 until 75) {
                val c = renderedTo.getColor(x,y)
                when {
                    c.red == 1f && Math.abs(c.alpha - 0.25f) < 0.01f -> reds++
                    c.alpha == 0f -> trans++
                    else -> other++
                }
            }
        }
        assert( reds > 0)
        assert( trans > 0)
        assert( other == 0)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(renderedTo)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\transformedHandleRender.png"))
        }
    }
}