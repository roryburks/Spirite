package rbJvm.glow.codec

import rb.glow.codec.CodecImageData
import rb.glow.codec.IImageCodec
import rb.glow.codec.RawImageFormat
import rb.vectrix.IMathLayer
import rb.vectrix.mathUtil.MathUtil
import rbJvm.glow.awt.RasterHelper
import rbJvm.vectrix.JvmMathLayer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.IntBuffer
import javax.imageio.ImageIO

/**
 * This Image Codec uses the Java ImageIO library to encode and decode PNG images.
 */
class NativeJavaPngImageCodec(val _mathLayer : IMathLayer = JvmMathLayer)
    : IImageCodec
{
    override fun encode(data: CodecImageData): ByteArray {
        when( data.format) {
            RawImageFormat.ARGB -> {
                // Some fairly inefficient stuff going on here.  Probably could have
                val biFormat = if( data.premultipliedAlpha) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB

                val bi = BufferedImage(data.width, data.height, biFormat)
                val internalStorage = RasterHelper.getDataStorageFromBi(bi) as IntArray

                internalStorage.forEachIndexed { index, i ->
                    val colorData = (data.raw[index*4 + 0].toInt() shl 0) +
                            (data.raw[index*4 + 1].toInt() shl 8) +
                            (data.raw[index*4 + 2].toInt() shl 16) +
                            (data.raw[index*4 + 3].toInt() shl 24)
                    internalStorage[index] = colorData
                }

                val baOut = ByteArrayOutputStream()
                ImageIO.write(bi, "png", baOut)
                return baOut.toByteArray()
            }
            else -> TODO("Not yet implemented: Format: ${data.format}")
        }
    }

    override fun decode(data: ByteArray): CodecImageData {
        TODO("Not yet implemented")
    }
}