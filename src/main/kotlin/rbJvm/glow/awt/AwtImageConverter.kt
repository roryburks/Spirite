package rbJvm.glow.awt

import rb.glow.IImageConverter
import rb.glow.exceptions.GLEException
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gl.IGL
import rb.glow.gle.IGLEngine
import rb.glow.img.IImage
import rbJvm.glow.jogl.JOGL.JOGLTextureSource
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.reflect.KClass

typealias NativeImage = ImageBI
typealias InternalImage = GLImage

class GLCreateTextureException(msg: String) : GLEException(msg)

class AwtImageConverter(
    val gleGetter : () -> IGLEngine?)
    : IImageConverter

{
    override fun convert(image: IImage, toType: KClass<*>) = when( toType) {
        GLImage::class -> convert<GLImage>(image)
        ImageBI::class -> convert<ImageBI>(image)
        else -> throw UnsupportedOperationException("Unrecognized")
    }

    override fun convertOrNull(image: IImage, toType: KClass<*>)= when( toType) {
        GLImage::class -> convertOrNull<GLImage>(image)
        ImageBI::class -> convertOrNull<ImageBI>(image)
        else -> null
    }

    val c = GLImage::class.java

    inline fun <reified T> convertOrNull(from: IImage) : T? {
        // Ugly
        if( from is T)
            return from

        when(T::class.java) {
            GLImage::class.java -> {
                val gle = gleGetter()
                val gl = gle!!.gl

                val tex = gl.createTexture() ?: throw GLCreateTextureException("Failed to create texture.")
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

    override fun convertToInternal( from: IImage) = convert<InternalImage>(from)

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