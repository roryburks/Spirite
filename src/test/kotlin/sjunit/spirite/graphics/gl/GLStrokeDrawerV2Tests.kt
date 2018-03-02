package sjunit.spirite.graphics.gl

import org.junit.Test
import sjunit.TestConfig
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.stroke.GLStrokeDrawerV2
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
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
        val image = drawer.batchDraw(drawPoints, StrokeParams(width = 5f), 100, 100)

        if( TestConfig.save)
            ImageIO.write(image.toBufferedImage(), "png", File("${TestConfig.saveLocation}\\glStrokeDraweV2_stroke.png"))
    }
}