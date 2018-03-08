package sjunit.spirite.base.graphics.gl


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

    @test fun SwitchesTargets() {
        val img1 = GLImage( 25,25,gle, false)
        val img2 = GLImage( 25, 25, gle, false)

        val gc1 = img1.graphics
        gc1.color = Colors.RED
        gc1.fillRect(0,0,25,25)


        val gc2 = img2.graphics
        gc2.color = Colors.BLUE
        gc2.fillRect(0,0,25,25)

        val img3 = img2.deepCopy() as GLImage

        assertEquals( Colors.RED.argb, img1.getARGB(10,10))
        assertEquals( Colors.BLUE.argb, img2.getARGB(10,10))
        if( TestConfig.save)
            ImageIO.write(img1.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\testSwitches1.png"))
        if( TestConfig.save)
            ImageIO.write(img3.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\testSwitches2.png"))
    }
}