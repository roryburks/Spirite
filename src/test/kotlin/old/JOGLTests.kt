package old

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import rbJvm.glow.jogl.JOGL
import sguiSwing.hybrid.Hybrid
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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