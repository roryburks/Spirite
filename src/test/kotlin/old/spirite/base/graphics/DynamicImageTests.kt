package old.spirite.base.graphics


import old.TestConfig
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import rb.glow.drawer
import rbJvm.glow.awt.AwtImageConverter
import rbJvm.glow.awt.ImageBI
import spirite.base.graphics.DynamicImage
import spirite.sguiHybrid.Hybrid
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tags(Tag("Old"),Tag("GPU"))
class DynamicImageTests {
    val gle = Hybrid.gle
    val imageConverter = AwtImageConverter{gle}

    @Test fun testWorks() {
        val dynamicImage = DynamicImage()
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            raw.graphics.drawer.drawLine(20.0,20.0,50.0,30.0)
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
    @Test fun testNullsGracefully() {
        val dynamicImage = DynamicImage()
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            // NOTHING
        })

        assertNull( dynamicImage.base)
    }
    @Test fun testMultipleDraws() {
        val dynamicImage = DynamicImage()
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            raw.graphics.drawer.drawLine(20.0,20.0,50.0,30.0)
        })
        dynamicImage.drawToImage(100, 100, drawer = { raw ->
            raw.graphics.drawer.drawLine(10.0,80.0,10.0,90.0)
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