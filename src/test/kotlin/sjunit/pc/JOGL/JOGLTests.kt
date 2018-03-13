package sjunit.pc.JOGL

import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGL.JOGLTexture
import spirite.pc.JOGL.JOGLProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test as test

class JOGLTests {
    @test fun StartsUp() {
        var x = false
        val gl = Hybrid.gl

        val tex = gl.createTexture()

        assertNotEquals(0, (tex as JOGLTexture).texId)
        assertEquals( 0, gl.getError())
    }
}