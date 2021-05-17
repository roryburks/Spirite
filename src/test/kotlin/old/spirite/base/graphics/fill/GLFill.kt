package old.spirite.base.graphics.fill

import old.TestConfig
import org.junit.jupiter.api.Test
import rb.glow.Colors
import rb.glow.drawer
import rb.glow.gl.GLImage
import rb.vectrix.mathUtil.d
import rbJvm.glow.awt.toBufferedImage
import sgui.swing.hybrid.EngineLaunchpoint
import spirite.specialRendering.fill.GLFill
import spirite.specialRendering.fill.V0FillArrayAlgorithm
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

class GLFillTests {
    @Test fun TestFill400() {
        TestFill(400)
    }
    @Test fun TestFill2000() {
        TestFill(2000)
    }

    fun TestFill( r: Int) {

        val image = GLImage(r, r, EngineLaunchpoint.gle, true)
        val w = image.width
        val h = image.height

        image.graphics.drawer.drawLine( 0.0, 0.0, w.d,h.d)

        println(measureTimeMillis {
            GLFill(V0FillArrayAlgorithm).fill(image.graphics, 3, 2, Colors.BLUE)
        })


        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\GLFILL_$r.png"))
    }
}