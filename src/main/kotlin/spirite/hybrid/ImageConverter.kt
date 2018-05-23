package spirite.hybrid

import spirite.base.graphics.IImage
import spirite.base.graphics.NillImage
import spirite.base.graphics.gl.GLEException
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.graphics.gl.IGL
import spirite.base.util.glu.GLC
import spirite.hybrid.MDebug.WarningType
import spirite.pc.JOGL.JOGL.JOGLTextureSource
import spirite.pc.graphics.ImageBI
import spirite.pc.toBufferedImage
import sun.awt.image.ByteInterleavedRaster
import sun.awt.image.IntegerInterleavedRaster
import java.nio.ByteBuffer
import java.nio.IntBuffer



class ImageConverter(
        val gle: GLEngine? = null
) {
    val c = GLImage::class.java

    inline fun <reified T> convertOrNull(from: IImage) : T? {
        // Ugly
        if( from is T)
            return from

        when(T::class.java) {
            GLImage::class.java -> {
                val gl = gle!!.getGl()

                val tex = gl.createTexture() ?: throw GLEException("Failed to create texture.")
                gl.bindTexture(GLC.TEXTURE_2D, tex)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)

                loadImageIntoGL(from, gl)
                return GLImage(tex, from.width, from.height, gle, false) as T
            }
            ImageBI::class.java -> {
                if( from is GLImage)
                    return ImageBI( from.toBufferedImage()) as T
            }
        }
        return null
    }
    inline fun <reified T> convert(from: IImage) : T = convertOrNull<T>(from) ?: throw Exception("Unsupported Conversion")

    fun convertToInternal( from: IImage) = convert<GLImage>(from)

    fun loadImageIntoGL( image: IImage, gl: IGL) {
        when( image) {
            is ImageBI -> {
                val raster = image.bi.raster

                when( raster) {
                    is ByteInterleavedRaster -> {
                        gl.texImage2D(
                                GLC.TEXTURE_2D,
                                0,
                                GLC.RGBA,
                                GLC.RGBA,
                                GLC.UNSIGNED_INT_8_8_8_8,
                                JOGLTextureSource( image.bi.width, image.bi.height, ByteBuffer.wrap(raster.dataStorage)))
                    }
                    is IntegerInterleavedRaster -> {
                        gl.texImage2D(
                                GLC.TEXTURE_2D,
                                0,
                                GLC.RGBA,
                                GLC.BGRA,
                                GLC.UNSIGNED_INT_8_8_8_8_REV,
                                JOGLTextureSource( image.bi.width, image.bi.height, IntBuffer.wrap(raster.dataStorage)))
                    }
                }
            }
        }
    }
}