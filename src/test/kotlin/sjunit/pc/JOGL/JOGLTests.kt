package sjunit.pc.JOGL

import rbJvm.glow.jogl.JOGL
import spirite.hybrid.Hybrid
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test as test

class JOGLTests {
    @test fun StartsUp() {
        var x = false
        val gl = Hybrid.gl

        val tex = gl.createTexture()

        assertNotEquals(0, (tex as JOGL.JOGLTexture).texId)
        assertEquals( 0, gl.getError())
    }
}