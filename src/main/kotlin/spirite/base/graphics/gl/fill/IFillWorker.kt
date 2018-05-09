package spirite.base.graphics.gl.fill

import spirite.base.graphics.fill.IFillArrayAlgorithm
import spirite.base.graphics.gl.FillAfterpassCall
import spirite.base.graphics.gl.GLImage
import spirite.base.graphics.gl.GLParameters
import spirite.base.util.f
import spirite.base.util.glu.GLC
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGL.JOGLTextureSource
import spirite.pc.gui.SColor
import java.io.File
import java.nio.IntBuffer

interface IFillWorker {
    fun fill(glImage: GLImage, x: Int, y: Int, color: SColor)
}

class GLFill(val filler: IFillArrayAlgorithm) : IFillWorker {
    override fun fill(glImage: GLImage, x: Int, y: Int, color: SColor) {
        val gle = glImage.engine
        val gl = gle.getGl()
        val w = glImage.width
        val h = glImage.height

        val data = filler.fill(glImage.toIntArray(), w, h, x, y, color.argb32) ?: return


        val _tex = gl.createTexture() ?: return
        val faW = (w-1) / 8 + 1
        val faH = (h-1) / 4 + 1
        gl.bindTexture(GLC.TEXTURE_2D, _tex)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)
        gl.texImage2D(
                GLC.TEXTURE_2D,
                0,
                GLC.R32UI,
                GLC.RED_INTEGER,
                GLC.UNSIGNED_INT,
                JOGLTextureSource( faW, faH, IntBuffer.wrap(data)))

        val img2 = GLImage(_tex,  faW, faH, gle)

        glImage.engine.setTarget(glImage)
        gle.applyPassProgram( FillAfterpassCall(color.rgbaComponent, w, h),
                GLParameters(w, h, texture1 = img2), null, 0f, 0f, w.f, h.f)

        img2.flush()
    }
}