package sjunit.rb.hydra


import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import rb.glow.drawer
import rb.glow.gl.GLImage
import rb.vectrix.shapes.RectI
import rbJvm.glow.util.ContentBoundsFinder
import spirite.sguiHybrid.Hybrid
import kotlin.test.assertEquals

class ContentBoundsFinderTest {
    val gle = Hybrid.gle

    @Tags(Tag("Old"), Tag("GPU"))
    @Test fun testContentBounds() {
        val img = GLImage(20, 20, gle)
        val gc = img.graphics
        gc.drawer.drawLine(5.0,5.0,13.0,17.0)
        val bounds = ContentBoundsFinder.findContentBounds(img, 0, true)

        assertEquals( RectI(5,5,8,12), bounds)
    }
}