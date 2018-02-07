package sjunit.spirite.graphics


import spirite.base.graphics.CapMethod.NONE
import spirite.base.graphics.JoinMethod.MITER
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.graphics.gl.GLParametersMutable
import spirite.base.graphics.gl.PolyRenderCall
import spirite.base.graphics.gl.PolyType.STRIP
import spirite.base.util.linear.Vec3
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService
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
                PolyRenderCall( Vec3(0f,1f,1f), 1f),
                listOf(10f,40f,10f, 40f),
                listOf(10f, 10f, 40f, 40f),
                4, STRIP,
                GLParametersMutable(50, 50),
                null)

        // Verify that a square has been drawn
        val black = 0xff000000.toInt()
        val gb = 0xff00ffff.toInt()
        assertEquals(gb, img.getRGB(15,15))
        assertEquals(gb, img.getRGB(15,35))
        assertEquals(gb, img.getRGB(35,35))
        assertEquals(gb, img.getRGB(35,15))
        assertEquals(black, img.getRGB( 5, 5))
        assertEquals(black, img.getRGB( 45, 5))
        assertEquals(black, img.getRGB( 5, 45))
        assertEquals(black, img.getRGB( 45, 45))
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
        val black = 0xff000000.toInt()
        val gb = 0xff00ffff.toInt()

        assertEquals(gb, img.getRGBDirect(1,1))
        assertEquals(gb, img.getRGBDirect(10,0))
        assertEquals(gb, img.getRGBDirect(49,40))
        assertEquals(gb, img.getRGBDirect(49,49))
        assertEquals(black, img.getRGBDirect(0,49))
        assertEquals(black, img.getRGBDirect(49,0))


        //val bi = img.toBufferedImage()
        //ImageIO.write(bi, "png", File("C:\\bucket\\t6.png"))
    }
}