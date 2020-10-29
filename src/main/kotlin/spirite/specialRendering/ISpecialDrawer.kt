package spirite.specialRendering

import rb.glow.GraphicsContext_old
import rb.glow.img.IImage
import rb.glow.Color
import rb.glow.Colors
import rb.glow.gl.GLImage
import rb.glow.gl.shader.programs.BasicCall
import rb.glow.gle.GLGraphicsContextOld
import rb.glow.using
import rb.vectrix.mathUtil.f
import spirite.base.brains.toolset.ColorChangeMode
import spirite.specialRendering.fill.GLFill
import spirite.specialRendering.fill.V0FillArrayAlgorithm

interface ISpecialDrawer {
    fun drawTransparencyBg(x: Int, y: Int, w: Int, h: Int, squareSize: Int, color1: Color = Colors.GRAY, color2: Color = Colors.LIGHT_GRAY)
    fun drawBounds(image: IImage, c: Int)
    fun invert()
    fun changeColor(from: Color, to: Color, mode: ColorChangeMode)
    fun fill(x: Int, y: Int, color: Color)
}

object SpecialDrawerFactory {
    fun makeSpecialDrawer( gc: GraphicsContext_old) = when( gc) {
        is GLGraphicsContextOld -> GLSpecialDrawer(gc)
        else -> TODO()
    }
}

class GLSpecialDrawer(private val _gc: GLGraphicsContextOld) : ISpecialDrawer{
    override fun fill(x: Int, y: Int, color: Color) {
        GLFill(V0FillArrayAlgorithm).fill(_gc, x, y, color)
    }

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        _gc.run {
            val buffer= GLImage(width, height, gle, premultiplied)

            cachedParams.texture1 = image
            gle.setTarget(buffer)
            gle.applyPassProgram(ChangeColorCall(from.rgbaComponent, to.rgbaComponent, mode),
                    cachedParams, null, 0f, 0f, width.f, height.f)

            cachedParams.texture1 = buffer
            gle.setTarget(image)
            gle.applyPassProgram(BasicCall(),
                    cachedParams, null, 0f, 0f, width.f, height.f)

            cachedParams.texture1 = null

            buffer.flush()
        }
    }

    override fun invert() {
        _gc.run {
            val buffer= GLImage(width, height, gle, premultiplied)

            cachedParams.texture1 = image
            gle.setTarget(buffer)
            gle.applyPassProgram(InvertCall(),
                    cachedParams, null, 0f, 0f, width.f, height.f)

            cachedParams.texture1 = buffer
            gle.setTarget(image)
            gle.applyPassProgram(BasicCall(),
                    cachedParams, null, 0f, 0f, width.f, height.f)

            buffer.flush()
            cachedParams.texture1 = null
        }
    }

    override fun drawTransparencyBg(x: Int, y: Int, w: Int, h: Int, squareSize: Int, color1: Color, color2: Color) {
        _gc.run {
            applyPassProgram(
                    GridCall(Colors.GRAY.rgbComponent, Colors.LIGHT_GRAY.rgbComponent, squareSize),cachedParams, transform, x.f, y.f, w.f, h.f)
        }
    }

    override fun drawBounds(image: IImage, c: Int) {
        _gc.run {
            using(GLImage(width, height, gle)) { buffer ->
                val gc = buffer.graphicsOld
                gc.clear()

                val texture = gle.converter.convertToGL(image, gle)
                val bufferParams = cachedParams.copy(texture1 = texture)
                gc.applyPassProgram(BasicCall(),
                        bufferParams, transform, 0f, 0f, image.width + 0f, image.height + 0f)

                bufferParams.texture1 = buffer
                applyPassProgram(BorderCall(c), bufferParams, null)
            }
        }
    }
}