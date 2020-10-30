package spirite.specialRendering.fill

import rb.glow.gl.GLC
import rb.glow.gl.GLGraphicsContext
import rb.glow.gl.GLImage
import rb.glow.gle.GLParameters
import rb.vectrix.linear.Vec4f
import rb.vectrix.mathUtil.f
import rb.vectrix.shapes.RectI
import rbJvm.glow.SColor
import rbJvm.glow.jogl.JOGL.JOGLTextureSource
import spirite.specialRendering.FillAfterpassCall
import java.nio.IntBuffer


class GLFill(val filler: IFillArrayAlgorithm)  {
    fun fill(gc: GLGraphicsContext, x: Int, y: Int, color: SColor) {
        val gle = gc.gle
        val gl = gle.gl
        val w = gc.width
        val h = gc.height

        val data = filler.fill(gc.toIntArray(), w, h, x, y, color.argb32) ?: return


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

        val img2 = GLImage(_tex, faW, faH, gle)

        gle.setTarget(gc.image)

        // Pass 1: clear the filled pixels
        if( color.alpha != 1.0f) {
            val params = GLParameters(w, h, texture1 = img2)
            params.setBlendMode(GLC.ZERO, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            gle.applyPassProgram(FillAfterpassCall(Vec4f(1f, 1f, 1f, 1f), w, h),
                    params, null, 0f, 0f, w.f, h.f)
        }
        // Pass 2: add the fill pixels
        if (color.alpha != 0.0f){
            gle.applyPassProgram(FillAfterpassCall(color.rgbaComponent, w, h),
                    GLParameters(w, h, texture1 = img2), null, 0f, 0f, w.f, h.f)
        }

        img2.flush()
    }
}


fun GLGraphicsContext.toIntArray(rect: RectI? = null) : IntArray{
    val rect2 = rect ?: RectI(0,0,width, height)
    gle.setTarget(image)

    if( rect2.wi <= 0 || rect2.hi <= 0)
        return IntArray(0)

    val gl = gle.gl
    val data = IntArray(rect2.wi * rect2.hi)
    val read = gl.makeInt32Source(data)
    gl.readnPixels(rect2.x1i, rect2.y1i, rect2.wi, rect2.hi, GLC.BGRA, GLC.UNSIGNED_INT_8_8_8_8_REV, 4*data.size, read )

    return data
}
