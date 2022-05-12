package rb.glow.codec

import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gle.IGLEngine
import rbJvm.glow.awt.GLCreateTextureException
import rbJvm.glow.jogl.JOGL
import java.nio.ByteBuffer

interface IGLImageDataCodecSvc {
    fun export(image: GLImage, gle: IGLEngine) : CodecImageData
    fun import(data: CodecImageData, gle: IGLEngine) : GLImage
}

// TODO: I don't know if the GLE Getter Approach is correct.  -RB 5-11-2022
object GLImageDataCodecSvc : IGLImageDataCodecSvc
{
    override fun export(image: GLImage, gle: IGLEngine): CodecImageData {
        val gl = gle.gl
        gle.setTarget(image)

        val array = IntArray(image.width * image.height)
        val intSource = gl.makeInt32Source(array)

        gl.readPixels(0, 0, image.width, image.height,
            GLC.BGRA,
            GLC.UNSIGNED_INT_8_8_8_8_REV,
            intSource)

        // Kotlin has extremely few tools for doing this well
        val byteArray = ByteArray(image.width * image.height * 4)
        array.forEachIndexed { index, i ->
            byteArray[index*4 + 0] = (i shr 0).toByte() // A
            byteArray[index*4 + 1] = (i shr 8).toByte() // R
            byteArray[index*4 + 2] = (i shr 16).toByte() // G
            byteArray[index*4 + 3] = (i shr 24).toByte() // B
        }

        return CodecImageData(
            image.width,
            image.height,
            byteArray,
            RawImageFormat.ARGB,
            image.premultiplied)
    }

    override fun import(data: CodecImageData, gle: IGLEngine): GLImage {
        when( data.format) {
            RawImageFormat.ARGB -> {
                val gl = gle.gl

                val tex = gl.createTexture() ?: throw GLCreateTextureException("Failed to create texture.")
                gl.bindTexture(GLC.TEXTURE_2D, tex)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MIN_FILTER, GLC.NEAREST)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_MAG_FILTER, GLC.NEAREST)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_S, GLC.CLAMP_TO_EDGE)
                gl.texParameteri(GLC.TEXTURE_2D, GLC.TEXTURE_WRAP_T, GLC.CLAMP_TO_EDGE)

                val texSrc = gl.createTextureSourceFromData(data.width, data.height, data.raw)

                gl.texImage2D(
                    GLC.TEXTURE_2D,
                    0,
                    GLC.RGBA,
                    GLC.BGRA,// !?
                    GLC.UNSIGNED_INT_8_8_8_8_REV, // ?
                    texSrc)


                return GLImage(tex, data.width, data.height, gle, data.premultipliedAlpha)
            }
            else -> throw NotImplementedError("Unsupported Codec Format: ${data.format}")
        }

    }
}