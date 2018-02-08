package sjunit.spirite.graphics

import spirite.base.graphics.CapMethod.NONE
import spirite.base.graphics.JoinMethod.MITER
import spirite.base.graphics.LineAttributes
import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.ChangeColorCall.ChangeMethod.IGNORE_ALPHA
import spirite.base.util.Colors
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec2i
import spirite.base.util.linear.Vec4
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService
import spirite.pc.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Test as test

class GLGraphicsTests {
    val gle =  GLEngine(JOGLProvider.getGL(), JClassScriptService())
    val save = true

    @test fun drawRect(){
        val img = GLImage( 30, 30, gle)
        val gc = img.graphics

        gc.color = Colors.RED
        gc.lineAttributes = LineAttributes(4f, NONE, MITER, null)
        gc.drawRect( 5, 5, 10, 10)

        val transparent = 0.toInt()
        val red = 0xffff0000.toInt()

        // Corners
        val corners = listOf(
                Vec2i( 5, 5),
                Vec2i( 5, 15),
                Vec2i( 15, 5),
                Vec2i( 15, 15))

        corners.forEach {
            println("$it")
            assertEquals(red, img.getARGB(it.x+1, it.y+1))
            assertEquals(red, img.getARGB(it.x-1, it.y-1))
            assertEquals(transparent, img.getARGB(it.x+3, it.y+3))
            assertEquals(transparent, img.getARGB(it.x-3, it.y-3))
        }
        assertEquals(transparent, img.getARGB(10, 10))
        assertEquals(transparent, img.getARGB(0, 0))
        assertEquals(transparent, img.getARGB(29, 29))
    }

    @test fun fillRect(){
        val img = GLImage( 30, 30, gle)
        val gc = img.graphics

        gc.color = Colors.RED
        gc.fillRect( 5, 5, 20, 20)

        val red = 0xffff0000.toInt()
        assertEquals(0, img.getARGB(4, 4))
        assertEquals(0, img.getARGB(4, 26))
        assertEquals(0, img.getARGB(26, 4))
        assertEquals(red, img.getARGB(15, 15))
        assertEquals(red, img.getARGB(6, 6))
        assertEquals(red, img.getARGB(6, 24))
        assertEquals(red, img.getARGB(24, 6))
        assertEquals(red, img.getARGB(24, 24))
    }

    @test fun fillPoly() {
        val img = GLImage( 50, 50, gle)
        val gc = img.graphics

        val xs = listOf(0f, 50f, 0f, 50f, 25f)
        val ys = listOf(0f, 40f, 40f, 0f, 50f)
        gc.color = Colors.RED
        gc.fillPolygon(xs, ys, 5)

        if( save) {
            ImageIO.write(img.toBufferedImage(), "png", File("C:/Bucket/fillPoly.png"))
        }
    }

    @test fun test() {
        val img = GLImage( 50, 50, gle)
        val gc = img.graphics

        val xs = listOf(0f, 50f, 0f, 50f, 25f)
        val ys = listOf(0f, 40f, 40f, 0f, 50f)
        gc.color = Colors.RED
        gc.fillPolygon(xs, ys, 5)

        val img2 = GLImage( 50, 50, gle)
        gle.setTarget(img2)
        gle.applyPassProgram( ChangeColorCall(Vec4(1f, 0f, 0f, 1f), Vec4( 0f, 1f, 0f, 1f), IGNORE_ALPHA),
                GLParametersMutable(50,50, texture1 = img), null, 0f, 0f, 50f, 50f)

        if( save) {
            ImageIO.write(img2.toBufferedImage(), "png", File("C:/Bucket/pass.png"))
        }
    }

}