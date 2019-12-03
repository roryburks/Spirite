package old

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import rbJvm.glow.jogl.JOGL
import spirite.hybrid.Hybrid
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test

class JOGLTests {
    @Tags(Tag("Old"), Tag("GPU"))
    @Test fun StartsUp() {
        var x = false
        val gl = Hybrid.gl

        val tex = gl.createTexture()

        assertNotEquals(0, (tex as JOGL.JOGLTexture).texId)
        assertEquals( 0, gl.getError())
    }
}