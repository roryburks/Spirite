package old.spirite.base.graphics.fill

import old.TestConfig
import org.junit.jupiter.api.Test
import rb.glow.color.Colors
import rb.glow.gl.GLImage
import rbJvm.glow.awt.toBufferedImage
import spirite.hybrid.EngineLaunchpoint
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

        image.graphics.drawLine( 0, 0, w,h)

        println(measureTimeMillis {
            GLFill(V0FillArrayAlgorithm).fill(image.graphics, 3, 2, Colors.BLUE)
        })


        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\GLFILL_$r.png"))
    }
}