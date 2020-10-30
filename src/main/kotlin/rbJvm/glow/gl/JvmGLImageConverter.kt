package rbJvm.glow.gl

import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gl.IGL
import rb.glow.gl.IGLImageConverter
import rb.glow.gle.IGLEngine
import rb.glow.img.IImage
import rbJvm.glow.awt.AwtImageConverter
import rbJvm.glow.awt.ImageBI
import rbJvm.glow.awt.RasterHelper
import rbJvm.glow.jogl.JOGL
import java.nio.ByteBuffer
import java.nio.IntBuffer

object JvmGLImageConverter : IGLImageConverter {
    override fun convertToGL(image: IImage, gle: IGLEngine): GLImage {
        return AwtImageConverter({gle}).convertToInternal(image) as GLImage
//        val gl = gle.gl
//        val tex = gl.createTexture() ?: throw GLException("Failed to create texture.")
//        gl.bindTexture(GLC.TEXTURE_2D, tex)
//        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
//        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
//        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
//        gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)
//
//        loadImageIntoGL(image, gl)
//        return GLImage(tex, image.width, image.height, gle, false)
    }

    fun loadImageIntoGL(image: IImage, gl: IGL) {
        when( image) {
            is ImageBI -> {

                val storage = RasterHelper.getDataStorageFromBi(image.bi)

                when( storage) {
                    is ByteArray -> {
                        gl.texImage2D(
                            GLC.TEXTURE_2D,
                            0,
                            GLC.RGBA,
                            GLC.RGBA,
                            GLC.UNSIGNED_INT_8_8_8_8,
                            JOGL.JOGLTextureSource(image.bi.width, image.bi.height, ByteBuffer.wrap(storage))
                        )
                    }
                    is IntArray -> {
                        gl.texImage2D(
                            GLC.TEXTURE_2D,
                            0,
                            GLC.RGBA,
                            GLC.BGRA,
                            GLC.UNSIGNED_INT_8_8_8_8_REV,
                            JOGL.JOGLTextureSource(image.bi.width, image.bi.height, IntBuffer.wrap(storage))
                        )
                    }

                }
            }
        }
    }
}