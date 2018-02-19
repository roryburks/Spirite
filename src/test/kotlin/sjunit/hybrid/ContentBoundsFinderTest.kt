package sjunit.hybrid


import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.util.linear.Rect
import spirite.hybrid.ContentBoundsFinder
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService
import kotlin.test.assertEquals
import org.junit.Test as test

class ContentBoundsFinderTest {
    val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())

    @test fun testContentBounds() {
        val img = GLImage(20, 20, gle)
        val gc = img.graphics
        gc.drawLine(5,5,13,17)
        val bounds = ContentBoundsFinder.findContentBounds(img, 0, true)

        assertEquals( Rect(5,5,8,12), bounds)
    }
}