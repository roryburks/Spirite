package sjunit.spirite.graphics


import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.GLEngine.ProgramType.PASS_BASIC
import spirite.base.graphics.gl.GLEngine.ProgramType.POLY_RENDER
import spirite.base.graphics.gl.PolyType.STRIP
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.PCUtil.toBufferedImage
import spirite.pc.resources.JClassScriptService
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Test as test

class GLEngineTests {
    @test fun StartsWithRealScripts() {
        val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())
    }

    @test fun TestBasicRendering() {
        val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())

        val img = GLImage(50, 50, gle)
        val gc = img.graphics
        gc.clear()

        val params = GLParametersMutable(50, 50, uniforms = listOf(
                GLUniform3f("uColor", 0f, 1f, 1f),
                GLUniform1f("uAlpha", 1f)
        ))

        gle.setTarget(img)
        gle.applyPolyProgram(
                POLY_RENDER,
                listOf(10f,40f,10f, 40f), listOf(10f, 10f, 40f, 40f),
                4, STRIP, params, null
        )

        // Verify that a square has been drawn
        val red = 0xffff0000.toInt()
        val gb = 0xff00ffff.toInt()
        assertEquals(gb, img.getRGB(15,15))
        assertEquals(gb, img.getRGB(15,35))
        assertEquals(gb, img.getRGB(35,35))
        assertEquals(gb, img.getRGB(35,15))
        assertEquals(red, img.getRGB( 5, 5))
        assertEquals(red, img.getRGB( 45, 5))
        assertEquals(red, img.getRGB( 5, 45))
        assertEquals(red, img.getRGB( 45, 45))
    }
}