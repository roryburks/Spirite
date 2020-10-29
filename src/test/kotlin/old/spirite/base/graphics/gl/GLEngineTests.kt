package old.spirite.base.graphics.gl


import old.TestConfig
import org.junit.jupiter.api.Test
import rb.glow.CapMethod.NONE
import rb.glow.JoinMethod.MITER
import rb.glow.Colors
import rb.glow.gl.GLImage
import rb.glow.gl.shader.programs.BasicCall
import rb.glow.gle.GLParameters
import rb.glow.gl.shader.programs.PolyRenderCall
import rb.glow.gle.PolyType.STRIP
import rb.glow.gl.shader.programs.RenderCall
import rb.glow.gl.shader.programs.RenderCall.RenderAlgorithm.*
import rb.vectrix.linear.Vec3f
import rb.vectrix.linear.Vec4f
import rb.vectrix.mathUtil.f
import rbJvm.glow.awt.toBufferedImage
import spirite.base.brains.toolset.ColorChangeMode.IGNORE_ALPHA
import spirite.base.pen.stroke.DrawPoints
import sguiSwing.hybrid.Hybrid
import spirite.specialRendering.*
import spirite.specialRendering.SquareGradientCall.GradientType.V
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.assertEquals

class GLEngineTests {
    val gle = Hybrid.gle

    @Test fun StartsWithRealScripts() {
    }

    @Test fun TestBasicRendering() {
        val img = GLImage(50, 50, gle)
        val gc = img.graphicsOld
        gc.clear()

        gle.setTarget(img)
        gle.applyPolyProgram(
                PolyRenderCall(Vec3f(0f, 1f, 1f), 1f),
                listOf(10f, 40f, 10f, 40f),
                listOf(10f, 10f, 40f, 40f),
                4, STRIP,
                GLParameters(50, 50),
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

        if( TestConfig.save)
            ImageIO.write(img.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\basicRendering.png"))
    }

    @Test fun TestComplexLineProgram() {
        val img = GLImage(50, 50, gle)
        val gc = img.graphicsOld
        gc.clear()

        val xs = listOf(0, 10, 50, 50)
        val ys = listOf(0, 0, 40, 50)

        val params = GLParameters(img.width, img.height)
        gle.setTarget(img)
        gle.applyComplexLineProgram(
                xs.map { it.toFloat() }, ys.map { it.toFloat() }, 4,
                NONE, MITER, false, 5f,
                Vec3f(0f, 1f, 1f), 1f,
                params, null)

        // Verify that a shape has been drawn
        val gb = 0xff00ffff.toInt()

        assertEquals(gb, img.getARGB(1,1))
        assertEquals(gb, img.getARGB(10,0))
        assertEquals(gb, img.getARGB(49,40))
        assertEquals(gb, img.getARGB(49,49))
        assertEquals(0, img.getARGB(0,49))
        assertEquals(0, img.getARGB(49,0))


        if( TestConfig.save)
            ImageIO.write(img.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\complexLineProgram.png"))
    }

    // Draws using all the shaders and outputs it to an image
    @Test fun writeOutPassShaders() {
        // Draw the base star
        val star = GLImage(50, 50, gle)
        val gc = star.graphicsOld

        val xs = listOf(0f, 50f, 0f, 50f, 25f)
        val ys = listOf(0f, 40f, 40f, 0f, 50f)
        gc.color = Colors.RED
        gc.fillPolygon(xs, ys, 5)

        val image = GLImage(500, 500, gle)
        image.graphics.clear()
        gle.setTarget(image)

        val params = GLParameters(500, 500, texture1 = star)

        // Call this one first to make sure the entire Uniform Arrays associated are properly reset
        gle.applyPassProgram(RenderCall(0.5f,
                listOf(
                        Pair(DISSOLVE, 4),
                        Pair(AS_COLOR, 0xff0000ff.toInt()))),
                params, null, 200f, 50f, 250f, 100f)
        gle.applyPassProgram(SquareGradientCall(0.5f, V),
                params, null, 0f, 0f, 50f, 50f)
        gle.applyPassProgram(ChangeColorCall(Vec4f(1f, 0f, 0f, 1f), Vec4f(0f, 1f, 0f, 1f), IGNORE_ALPHA),
                params, null, 50f, 0f, 100f, 50f)
        gle.applyPassProgram(GridCall(Vec3f(0.25f, 0.25f, 0.25f), Vec3f(0.5f, 0.5f, 0.5f), 4),
                params, null, 100f, 0f, 150f, 50f)
        gle.applyPassProgram(BasicCall(),
                params, null, 150f, 0f, 200f, 50f)
        gle.applyPassProgram(BorderCall(3),
                params, null, 200f, 0f, 250f, 50f)
        gle.applyPassProgram(InvertCall(),
                params, null, 250f, 0f, 300f, 50f)
        gle.applyPassProgram(RenderCall(0.5f, emptyList()),
                params, null, 0f, 50f, 50f, 100f)
        gle.applyPassProgram(RenderCall(0.5f, listOf(Pair(AS_COLOR, 0xff0000ff.toInt()))),
                params, null, 50f, 50f, 100f, 100f)
        gle.applyPassProgram(RenderCall(0.5f, listOf(Pair(AS_COLOR_ALL, 0xff00ffff.toInt()))),
                params, null, 100f, 50f, 150f, 100f)
        gle.applyPassProgram(RenderCall(0.5f, listOf(Pair(DISSOLVE, 4))),
                params, null, 150f, 50f, 200f, 100f)


        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\shaders.png"))
    }

    @Test fun doStroke() {
        val image = GLImage(100, 100, gle)

        gle.setTarget(image)
        val drawPoints = DrawPoints(
                FloatArray(100) { it.f },
                FloatArray(100) { it.f },
                FloatArray(100) { 1f - abs(50 - it) / 50f }
        )

        println(drawPoints)

        // TODO

        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\stroke.png"))
    }
}