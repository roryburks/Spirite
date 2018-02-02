package sjunit.spirite.graphics


import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.pc.JOGL.JOGLProvider
import kotlin.test.assertEquals
import org.junit.Test as test

class GLImageTests {
    @test fun BasicGLFunctionality() {
        val gle = GLEngine(JOGLProvider.getGL())
        val glimage = GLImage(10,10,gle)

        glimage.graphics.clear()

        val color = glimage.getRGB(2,2)

        assertEquals(0xffff0000.toInt(), color)
    }
}