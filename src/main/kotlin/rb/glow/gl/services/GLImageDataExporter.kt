package rb.glow.gl.services

import rb.file.imgCodec.CodecImageData
import rb.file.imgCodec.RawImageFormat
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gle.IGLEngine

interface IGLImageDataExporter {
    fun export(image: GLImage, gle: IGLEngine) : CodecImageData
}

// TODO: I don't know if the GLE Getter Approach is correct.  -RB 5-11-2022
object GLImageDataExporter : IGLImageDataExporter
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
            // Re-mangling from ARGB to RGBA anyway
            byteArray[index*4 + 3] = (i shr 0).toByte() // A
            byteArray[index*4 + 0] = (i shr 8).toByte() // R
            byteArray[index*4 + 1] = (i shr 16).toByte() // G
            byteArray[index*4 + 2] = (i shr 24).toByte() // B
        }

        return CodecImageData(
            byteArray,
            RawImageFormat.RGBA,
            image.premultiplied)
    }
}