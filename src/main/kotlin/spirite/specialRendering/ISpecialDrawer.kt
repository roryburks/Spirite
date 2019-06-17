package spirite.specialRendering

import rb.glow.GraphicsContext
import rb.glow.IImage
import rb.glow.color.Color
import rb.glow.color.Colors
import rb.glow.gl.GLImage
import rb.glow.gle.BasicCall
import rb.glow.gle.GLGraphicsContext
import rb.glow.using
import rb.vectrix.mathUtil.f

interface ISpecialDrawer {
    fun drawTransparencyBg(x: Int, y: Int, w: Int, h: Int, squareSize: Int, color1: Color = Colors.GRAY, color2: Color = Colors.LIGHT_GRAY)
    fun drawBounds(image: IImage, c: Int)
}

object SpecialDrawerFactory {
    fun makeSpecialDrawer( gc: GraphicsContext) = when( gc) {
        is GLGraphicsContext -> GLSpecialDrawer(gc)
        else -> TODO()
    }
}

class GLSpecialDrawer(private val _gc: GLGraphicsContext) : ISpecialDrawer{
    override fun drawTransparencyBg(x: Int, y: Int, w: Int, h: Int, squareSize: Int, color1: Color, color2: Color) {
        _gc.run {
            applyPassProgram(
                    GridCall(Colors.GRAY.rgbComponent, Colors.LIGHT_GRAY.rgbComponent, squareSize),cachedParams, transform, x.f, y.f, w.f, h.f)
        }
    }

    override fun drawBounds(image: IImage, c: Int) {
        _gc.run {
            using(GLImage(width, height, gle)) { buffer ->
                val gc = buffer.graphics
                gc.clear()

                val texture = gle.converter.convert(image, GLImage::class) as GLImage // ImageConverter(gle).convert<GLImage>(image)
                val bufferParams = cachedParams.copy(texture1 = texture)
                gc.applyPassProgram(BasicCall(),
                        bufferParams, transform, 0f, 0f, image.width + 0f, image.height + 0f)

                bufferParams.texture1 = buffer
                applyPassProgram(BorderCall(c), bufferParams, null)
            }
        }
    }
}