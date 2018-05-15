package spirite.base.graphics

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.fill.V0FillArrayAlgorithm
import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.fill.GLFill
import spirite.base.util.Color
import spirite.base.util.f
import spirite.base.util.glu.GLC

class GLDrawer( val image: GLImage) : IDrawer {
    val params by lazy {
        val p = GLParameters(image.width, image.height)
        p.setBlendMode(GLC.ONE, GLC.ZERO, GLC.FUNC_ADD)
        p
    }

    override fun invert() {
        val gle = image.engine
        val buffer= GLImage(image.width, image.height,gle, image.premultiplied)

        params.texture1 = image
        gle.setTarget(buffer)
        gle.applyPassProgram(InvertCall(),
                params, null, 0f, 0f, image.width.f, image.height.f)

        params.texture1 = buffer
        gle.setTarget(image)
        image.engine.applyPassProgram(BasicCall(),
                params, null, 0f, 0f, image.width.f, image.height.f)

        buffer.flush()
        params.texture1 = null
    }

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        val gle = image.engine
        val buffer= GLImage(image.width, image.height,gle, image.premultiplied)

        params.texture1 = image
        gle.setTarget(buffer)
        image.engine.applyPassProgram(ChangeColorCall(from.rgbaComponent, to.rgbaComponent, mode),
                params, null, 0f, 0f, image.width.f, image.height.f)

        params.texture1 = buffer
        gle.setTarget(image)
        image.engine.applyPassProgram(BasicCall(),
                params, null, 0f, 0f, image.width.f, image.height.f)

        buffer.flush()
    }

    override fun fill(x: Int, y: Int, color: Color) {
        GLFill(V0FillArrayAlgorithm).fill(image, x, y, color)
    }
}