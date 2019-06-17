package sjunit.spirite.base.graphics.fill

import rb.glow.color.Colors
import sjunit.TestConfig
import spirite.base.graphics.fill.V0FillArrayAlgorithm
import rb.glow.gl.GLImage
import spirite.specialRendering.fill.GLFill
import spirite.hybrid.EngineLaunchpoint
import spirite.pc.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis
import org.junit.Test as test

class GLFillTests {
    @test fun TestFill400() {
        TestFill(400)
    }
    @test fun TestFill2000() {
        TestFill(2000)
    }

    fun TestFill( r: Int) {

        val image = GLImage(r, r, EngineLaunchpoint.gle, true)
        val w = image.width
        val h = image.height

        image.graphics.drawLine( 0, 0, w,h)

        println(measureTimeMillis {
            GLFill(V0FillArrayAlgorithm).fill(image, 3, 2, Colors.BLUE)
        })


        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\GLFILL_$r.png"))
    }
}