package rbJvm.glow.awt

import rb.glow.IImage
import rb.glow.exceptions.GLEException
import rb.glow.gl.GLC
import rb.glow.gl.IGL
import rb.glow.gl.GLImage
import rb.glow.gle.IGLEngine
import rb.glow.gle.IImageConverter
import rbJvm.glow.jogl.JOGL.JOGLTextureSource
import spirite.hybrid.Hybrid
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.UnsupportedOperationException
import kotlin.reflect.KClass

typealias NativeImage = ImageBI
typealias InternalImage = GLImage

object AwtIImageConverter : IImageConverter
{
    override fun convert(image: IImage, toType: KClass<*>) = when( toType) {
        GLImage::class -> AwtImageConverter(Hybrid.gle).convert<GLImage>(image)
        ImageBI::class -> AwtImageConverter(Hybrid.gle).convert<ImageBI>(image)
        else -> throw UnsupportedOperationException("Unrecognized")
    }

}

class AwtImageConverter(
        val gle: IGLEngine? = null
) {
    val c = GLImage::class.java

    inline fun <reified T> convertOrNull(from: IImage) : T? {
        // Ugly
        if( from is T)
            return from

        when(T::class.java) {
            GLImage::class.java -> {
                val gl = gle!!.gl

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
                    return ImageBI(from.toBufferedImage()) as T
            }
        }
        return null
    }
    inline fun <reified T> convert(from: IImage) : T = convertOrNull<T>(from) ?: throw Exception("Unsupported Conversion")

    fun convertToInternal( from: IImage) = convert<InternalImage>(from)

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
                                JOGLTextureSource(image.bi.width, image.bi.height, ByteBuffer.wrap(storage)))
                    }
                    is IntArray -> {
                        gl.texImage2D(
                                GLC.TEXTURE_2D,
                                0,
                                GLC.RGBA,
                                GLC.BGRA,
                                GLC.UNSIGNED_INT_8_8_8_8_REV,
                                JOGLTextureSource( image.bi.width, image.bi.height, IntBuffer.wrap(storage)))
                    }

                }
            }
        }
    }
}