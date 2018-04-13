package spirite.base.graphics

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.graphics.gl.ChangeColorCall
import spirite.base.graphics.gl.GLImage
import spirite.base.graphics.gl.GLParameters
import spirite.base.graphics.gl.InvertCall
import spirite.base.util.Color
import spirite.base.util.f
import spirite.base.util.linear.Transform

class GLDrawer( val image: GLImage) : IDrawer {
    val params by lazy { GLParameters(image.width, image.height) }

    override fun invert() {
        image.engine.applyPassProgram(InvertCall(),
                params, null, 0f, 0f, image.width.f, image.height.f)
    }

    override fun changeColor(from: Color, to: Color, mode: ColorChangeMode) {
        image.engine.applyPassProgram(ChangeColorCall(from.rgbaComponent, to.rgbaComponent, mode),
                params,null, 0f, 0f, image.width.f, image.height.f)
    }

    override fun fill(x: Int, y: Int, color: Color, mask: IImage?, maskTransform: Transform?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}