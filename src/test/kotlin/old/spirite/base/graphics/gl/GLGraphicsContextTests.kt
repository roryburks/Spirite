package old.spirite.base.graphics.gl

import rb.glow.CapMethod.NONE
import rb.glow.JoinMethod.MITER
import rb.glow.LineAttributes
import rb.glow.color.Colors
import rb.glow.gl.GLImage
import rb.vectrix.linear.Vec2i
import rbJvm.glow.awt.toBufferedImage
import old.TestConfig
import spirite.hybrid.Hybrid
import spirite.specialRendering.SpecialDrawerFactory
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test as test

class GLGraphicsContextTests {
    val gle = Hybrid.gle

    @test fun drawBounds() {
        val img = GLImage(30, 30, gle)

        val gc = img.graphics
        gc.color = Colors.RED
        gc.fillRect( 5, 5, 10, 10)

        val toImage = GLImage(30, 30, gle)
        SpecialDrawerFactory.makeSpecialDrawer(toImage.graphics).drawBounds( img, 101101)

        if( TestConfig.save)
            ImageIO.write(toImage.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\gc_drawBounds.png"))
    }
    @test fun drawImage() {
        val subImg = GLImage(30, 30, gle)

        val gc = subImg.graphics
        gc.color = Colors.RED
        gc.fillRect( 5, 5, 10, 10)

        val toImage = GLImage(30, 30, gle)
        val togc = toImage.graphics
        togc.alpha = 0.5f
        togc.renderImage( subImg, 5, 5)

        val color = toImage.getColor(19,19)
        assertEquals(1.0f, color.red)

        if( TestConfig.save)
            ImageIO.write(toImage.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\gc_drawImage.png"))
    }

    @test fun drawRect(){
        val img = GLImage(30, 30, gle)
        val gc = img.graphics

        //gc.setComposite(gc.composite, 0.5f)
        gc.color = Colors.RED
        gc.lineAttributes = LineAttributes(4f, NONE, MITER, null)
        gc.drawRect( 5, 5, 10, 10)

        val transparent = 0.toInt()
        val red = 0xffff0000.toInt()

        // Corners
        val corners = listOf(
                Vec2i(5, 5),
                Vec2i(5, 15),
                Vec2i(15, 5),
                Vec2i(15, 15))

        corners.forEach {
            assertEquals(red, img.getARGB(it.xi+1, it.yi+1))
            assertEquals(red, img.getARGB(it.xi-1, it.yi-1))
            assertEquals(transparent, img.getARGB(it.xi+3, it.yi+3))
            assertEquals(transparent, img.getARGB(it.xi-3, it.yi-3))
        }
        assertEquals(transparent, img.getARGB(10, 10))
        assertEquals(transparent, img.getARGB(0, 0))
        assertEquals(transparent, img.getARGB(29, 29))

        if( TestConfig.save)
            ImageIO.write(img.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\gc_drawRect.png"))
    }

    @test fun fillRect(){
        val img = GLImage(30, 30, gle)
        val gc = img.graphics

        gc.color = Colors.RED
        gc.fillRect( 5, 5, 20, 20)

        val red = 0xffff0000.toInt()
        assertEquals(0, img.getARGB(4, 4))
        assertEquals(0, img.getARGB(4, 26))
        assertEquals(0, img.getARGB(26, 4))
        assertEquals(red, img.getARGB(15, 15))
        assertEquals(red, img.getARGB(6, 6))
        assertEquals(red, img.getARGB(6, 24))
        assertEquals(red, img.getARGB(24, 6))
        assertEquals(red, img.getARGB(24, 24))

        if( TestConfig.save)
            ImageIO.write(img.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\gc_fillRect.png"))
    }

    @test fun fillPoly() {
        val img = GLImage(50, 50, gle)
        val gc = img.graphics

        val xs = listOf(0f, 50f, 0f, 50f, 25f)
        val ys = listOf(0f, 40f, 40f, 0f, 50f)
        gc.color = Colors.RED
        gc.fillPolygon(xs, ys, 5)

        if( TestConfig.save)
            ImageIO.write(img.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\gc_fillPoly.png"))
    }

}