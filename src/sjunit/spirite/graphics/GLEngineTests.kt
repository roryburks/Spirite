package sjunit.spirite.graphics


import spirite.base.graphics.CapMethod.NONE
import spirite.base.graphics.JoinMethod.MITER
import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.ChangeColorCall.ChangeMethod.IGNORE_ALPHA
import spirite.base.graphics.gl.PolyType.STRIP
import spirite.base.graphics.gl.RenderCall.RenderAlgorithm.*
import spirite.base.graphics.gl.SquareGradientCall.GradientType
import spirite.base.graphics.gl.stroke.V2PenDrawer
import spirite.base.pen.DrawPoints
import spirite.base.util.ColorARGB32
import spirite.base.util.Colors
import spirite.base.util.linear.Vec3
import spirite.base.util.linear.Vec4
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService
import spirite.pc.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.assertEquals
import org.junit.Test as test

class GLEngineTests {
    val gle =  GLEngine(JOGLProvider.getGL(), JClassScriptService())

    @test fun StartsWithRealScripts() {
    }

    @test fun TestBasicRendering() {
        val img = GLImage(50, 50, gle)
        val gc = img.graphics
        gc.clear()

        gle.setTarget(img)
        gle.applyPolyProgram(
                PolyRenderCall(Vec3(0f, 1f, 1f), 1f),
                listOf(10f, 40f, 10f, 40f),
                listOf(10f, 10f, 40f, 40f),
                4, STRIP,
                GLParametersMutable(50, 50),
                null)

        // Verify that a square has been drawn
        val gb = 0xff00ffff.toInt()
        assertEquals(gb, img.getARGB(15, 15))
        assertEquals(gb, img.getARGB(15, 35))
        assertEquals(gb, img.getARGB(35, 35))
        assertEquals(gb, img.getARGB(35, 15))
        assertEquals(0, img.getARGB(5, 5))
        assertEquals(0, img.getARGB(45, 5))
        assertEquals(0, img.getARGB(5, 45))
        assertEquals(0, img.getARGB(45, 45))
    }

    @test fun TestComplexLineProgram() {
        val img = GLImage(50, 50, gle)
        val gc = img.graphics
        gc.clear()

        val xs = listOf(0, 10, 50, 50)
        val ys = listOf(0, 0, 40, 50)

        val params = GLParametersMutable(img.width, img.height)
        gle.setTarget(img)
        gle.applyComplexLineProgram(
                xs.map { it.toFloat() }, ys.map { it.toFloat() }, 4,
                NONE, MITER, false, 5f,
                Vec3(0f, 1f, 1f), 1f,
                params, null)

        // Verify that a shape has been drawn
        val gb = 0xff00ffff.toInt()

        assertEquals(gb, img.getARGB(1,1))
        assertEquals(gb, img.getARGB(10,0))
        assertEquals(gb, img.getARGB(49,40))
        assertEquals(gb, img.getARGB(49,49))
        assertEquals(0, img.getARGB(0,49))
        assertEquals(0, img.getARGB(49,0))


        //val bi = img.toBufferedImage()
        //ImageIO.write(bi, "png", File("C:\\bucket\\t6.png"))
    }

    // Draws using all the shaders and outputs it to an image
    @test fun writeOutPassShaders() {
        // Draw the base star
        val star = GLImage( 50, 50, gle)
        val gc = star.graphics

        val xs = listOf(0f, 50f, 0f, 50f, 25f)
        val ys = listOf(0f, 40f, 40f, 0f, 50f)
        gc.color = Colors.RED
        gc.fillPolygon(xs, ys, 5)

        val image = GLImage( 500, 500, gle)
        image.graphics.clear()
        gle.setTarget(image)

        val params = GLParametersMutable(500,500, texture1 = star)

        gle.applyPassProgram( SquareGradientCall(0.5f, GradientType.V),
                params, null, 0f, 0f, 50f, 50f)
        gle.applyPassProgram( ChangeColorCall(Vec4(1f, 0f, 0f, 1f), Vec4( 0f, 1f, 0f, 1f), IGNORE_ALPHA),
                params, null, 50f, 0f, 100f, 50f)
        gle.applyPassProgram( GridCall(Vec3(0.25f, 0.25f, 0.25f), Vec3( 0.5f, 0.5f, 0.5f), 4),
                params, null, 100f, 0f, 150f, 50f)
        gle.applyPassProgram( BasicCall(),
                params, null, 150f, 0f, 200f, 50f)
        gle.applyPassProgram( BorderCall(3),
                params, null, 200f, 0f, 250f, 50f)
        gle.applyPassProgram( InvertCall(),
                params, null, 250f, 0f, 300f, 50f)
        gle.applyPassProgram( RenderCall(0.5f, 0, true, STRAIGHT_PASS),
                params, null, 0f, 50f, 50f, 100f)
        gle.applyPassProgram( RenderCall(0.5f, 0xff0000ff.toInt(), true, AS_COLOR),
                params, null, 50f, 50f, 100f, 100f)
        gle.applyPassProgram( RenderCall(0.5f, 0xff00ffff.toInt(), true, AS_COLOR_ALL),
                params, null, 100f, 50f, 150f, 100f)
        gle.applyPassProgram( RenderCall(0.5f, 4, true, DISOLVE),
                params, null, 150f, 50f, 200f, 100f)


        ImageIO.write(image.toBufferedImage(), "png", File("C:/Bucket/shaders.png"))
    }

    @test fun doStroke() {
        val image = GLImage( 100, 100, gle)

        gle.setTarget(image)
        val drawPoints = DrawPoints(
                FloatArray(100, {it.toFloat()}),
                FloatArray(100, {it.toFloat()}),
                FloatArray(100, {1f - abs(50 - it)/50f})
        )
        V2PenDrawer.drawStroke(drawPoints, 5f, gle, ColorARGB32(0xffff0000.toInt()), GLParametersMutable(image.width, image.height), null)
        ImageIO.write(image.toBufferedImage(), "png", File("C:/Bucket/stroke.png"))
    }
}