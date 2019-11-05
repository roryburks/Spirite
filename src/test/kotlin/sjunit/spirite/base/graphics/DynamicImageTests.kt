package sjunit.spirite.base.graphics


import rbJvm.glow.awt.AwtImageConverter
import rbJvm.glow.awt.ImageBI
import sjunit.TestConfig
import spirite.base.graphics.DynamicImage
import spirite.hybrid.Hybrid
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test as test

class DynamicImageTests {
    val gle = Hybrid.gle
    val imageConverter = AwtImageConverter{gle}

    @test fun testWorks() {
        val dynamicImage = DynamicImage()
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            raw.graphics.drawLine(20,20,50,30)
        })

        assertEquals(20, dynamicImage.xOffset)
        assertEquals(20, dynamicImage.yOffset)
        assertEquals(10, dynamicImage.height)
        assertEquals(30, dynamicImage.width)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(dynamicImage.base!!)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicImage.png"))
        }
    }
    @test fun testNullsGracefully() {
        val dynamicImage = DynamicImage()
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            // NOTHING
        })

        assertNull( dynamicImage.base)
    }
    @test fun testMultipleDraws() {
        val dynamicImage = DynamicImage()
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            raw.graphics.drawLine(20,20,50,30)
        })
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            raw.graphics.drawLine(10,80,10,90)
        })

        assertEquals(9, dynamicImage.xOffset)   // should really be 10
        assertEquals(20, dynamicImage.yOffset)
        assertEquals(41, dynamicImage.width)    // should really be 40
        assertEquals(70, dynamicImage.height)

        if( TestConfig.save) {
            val imageBI = imageConverter.convert<ImageBI>(dynamicImage.base!!)
            ImageIO.write(imageBI.bi, "png", File("${TestConfig.saveLocation}\\dynamicImage.png"))
        }
    }
}