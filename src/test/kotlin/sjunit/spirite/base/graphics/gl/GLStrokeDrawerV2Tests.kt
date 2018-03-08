package sjunit.spirite.base.graphics.gl

import org.junit.Test
import sjunit.TestConfig
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.graphics.gl.stroke.GLStrokeDrawerV2
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.ERASE
import spirite.base.util.Colors
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService
import spirite.pc.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

class GLStrokeDrawerV2Tests {
    val gle =  GLEngine(JOGLProvider.getGL(), JClassScriptService())
    val drawer = GLStrokeDrawerV2(gle)

    @Test
    fun drawsStroke() {
        val drawPoints = DrawPoints(
                FloatArray(100, { it.toFloat() }),
                FloatArray(100, { it.toFloat() }),
                FloatArray(100, { 1f - abs(50 - it) / 50f })
        )
        val image = GLImage(100,100, gle)
        drawer.batchDraw(image.graphics, drawPoints, StrokeParams(Colors.GREEN, width = 5f, alpha = 0.5f), 100, 100)

        //val image = drawer.batchDraw(drawPoints, StrokeParams(width = 5f), 100, 100)

        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\glStrokeDrawerV2_stroke.png"))
    }

    @Test
    fun drawsStrokeErase() {
        val drawPoints = DrawPoints(
                FloatArray(100, { it.toFloat() }),
                FloatArray(100, { it.toFloat() }),
                FloatArray(100, { 1f - abs(50 - it) / 50f })
        )
        val image = GLImage(100,100, gle)
        val gc = image.graphics
        gc.color = Colors.GRAY
        gc.fillRect(0,0,100,100)

        drawer.batchDraw(image.graphics, drawPoints, StrokeParams(Colors.GREEN, width = 5f, alpha = 0.75f, method = ERASE), 100, 100)

        //val image = drawer.batchDraw(drawPoints, StrokeParams(width = 5f), 100, 100)

        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\glStrokeDrawerV2_erase.png"))
    }
}