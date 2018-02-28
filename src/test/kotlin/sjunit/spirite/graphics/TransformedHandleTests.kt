package sjunit.spirite.graphics


import io.mockk.mockk
import sjunit.TestConfig
import spirite.base.brains.Settings.ISettingsManager
import spirite.base.brains.palette.IPaletteManager
import spirite.base.graphics.RenderMethod
import spirite.base.graphics.RenderMethodType.COLOR_CHANGE_HUE
import spirite.base.graphics.RenderMethodType.DISOLVE
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.ImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.util.Colors
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.MutableTransform.Companion
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

    val renderEngine = mockk<IRenderEngine>()
    val settingsManager = mockk<ISettingsManager>()
    val paletteManager = mockk<IPaletteManager>()
    val workspace = ImageWorkspace(renderEngine, settingsManager, paletteManager)

    @test fun testStacks() {
        val tf = TransformedHandle(
                MediumHandle(workspace, 0),
                MutableTransform.RotationMatrix(30f),
                0.5f,
                RenderMethod(COLOR_CHANGE_HUE, Colors.RED.argb))
        val tf2 = TransformedHandle(
                MediumHandle(workspace, 0),
                MutableTransform.TranslationMatrix(-10f,-10f),
                0.5f,
                RenderMethod(DISOLVE, Colors.RED.argb))

        val tf3 = tf.stack(tf2)

        val tExpected = MutableTransform.RotationMatrix(30f)
        tExpected.preTranslate(-10f, -10f)

        assertEquals(COLOR_CHANGE_HUE,tf3.renderRhubric.methods[0].methodType)
        assertEquals(DISOLVE,tf3.renderRhubric.methods[1].methodType)
        assertEquals( tExpected, tf3.transform)
        assertEquals( 0.25f, tf3.alpha)
    }

    @test fun renders() {
        val tf = TransformedHandle(
                MediumHandle(workspace, 0),
                MutableTransform.RotationMatrix(30f),
                0.5f,
                RenderMethod(COLOR_CHANGE_HUE, Colors.RED.argb))
        val tf2 = TransformedHandle(
                MediumHandle(workspace, 0),
                MutableTransform.TranslationMatrix(-10f,-10f),
                0.5f,
                RenderMethod(DISOLVE, 0b1111_00001111_00001111_00001111))

        val tf3 = tf.stack(tf2)

        val square = Hybrid.imageCreator.createImage(100,100)
        val squareGC = square.graphics
        squareGC.color = Colors.GREEN
        squareGC.fillRect(25,25,50,50)

        val renderedTo = Hybrid.imageCreator.createImage(100,100)
        val toGC = renderedTo.graphics
        toGC.alpha = tf3.alpha
        toGC.renderImage(square, 0, 0, tf3.renderRhubric)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(renderedTo)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\transformedHandleRender.png"))
        }
    }

}