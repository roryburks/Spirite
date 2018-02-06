package sjunit.spirite.graphics


import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.resources.IScriptService
import spirite.pc.JOGL.JOGLProvider
import java.awt.image.BufferedImage
import kotlin.test.assertEquals
import spirite.pc.PCUtil.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Test as test

class GLImageTests {
    @test fun BasicGLFunctionality() {
        val gle = GLEngine(JOGLProvider.getGL(), TrivialScriptService())
        val glimage = GLImage(10,10,gle)

        glimage.graphics.clear()

        val color = glimage.getRGB(2,2)

        assertEquals(0xffff0000.toInt(), color)
    }
    @test fun SuperBasicFunctionality() {
        val gle = GLEngine(JOGLProvider.getGL(), TrivialScriptService())
        val glimage = GLImage(10,12,gle)

        assertEquals( 10, glimage.width)
        assertEquals( 12, glimage.height)
    }

    @test fun DeepCopy() {
        val gle = GLEngine(JOGLProvider.getGL(),TrivialScriptService())
        val glimage = GLImage(10,10,gle)

        glimage.graphics.clear()

        val gli2 = glimage.deepCopy()
        val color = gli2.getRGB(2,2)

        assertEquals(0xffff0000.toInt(), color)
    }
    @test fun TestConvertToBi() {
        val gle = GLEngine(JOGLProvider.getGL(), TrivialScriptService())
        val glimage = GLImage(10,12,gle)
        glimage.graphics.clear()

        assertEquals( 10, glimage.width)
        assertEquals( 12, glimage.height)

        val bi = glimage.toBufferedImage()
        ImageIO.write(bi, "png", File("C:\\bucket\\t6.png"))
    }
}