package sjunit.spirite.graphics


import sjunit.TestConfig
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.util.Colors
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService
import spirite.pc.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Test as test

class GLImageTests {
    val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())

    @test fun BasicGLFunctionality() {
        val glimage = GLImage(10,10,gle)

        val gc = glimage.graphics
        //gc.clear()
        gc.color = Colors.RED
        gc.fillRect(0, 0, 10, 10)

        val color = glimage.getARGB(2,2)

        assertEquals(Colors.RED.argb, color)
    }
    @test fun SuperBasicFunctionality() {
        val glimage = GLImage(10,12,gle)

        assertEquals( 10, glimage.width)
        assertEquals( 12, glimage.height)
    }

    @test fun DeepCopy() {
        val glimage = GLImage(10,10,gle)

        val gc = glimage.graphics
        gc.clear()
        gc.color = Colors.RED
        gc.fillRect(0, 0, 10, 10)

        val gli2 = glimage.deepCopy()
        val color = gli2.getARGB(2,2)

        assertEquals(Colors.RED.argb, color)
    }
    @test fun TestConvertToBi() {
        val glimage = GLImage(10,12,gle)
        glimage.graphics.clear()

        assertEquals( 10, glimage.width)
        assertEquals( 12, glimage.height)

        val bi = glimage.toBufferedImage()

        if( TestConfig.save)
            ImageIO.write(bi, "png", File("${TestConfig.saveLocation}\\testConversion.png"))
    }
}