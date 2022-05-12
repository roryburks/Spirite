package rbJvm.glow.codec

import rb.extendo.kiebabo.BitPacking
import rb.glow.codec.CodecImageData
import rb.glow.codec.IImageCodec
import rb.glow.codec.CodecImageFormat
import rb.vectrix.IMathLayer
import rbJvm.glow.awt.RasterHelper
import rbJvm.vectrix.JvmMathLayer
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.management.Query.and

/**
 * This Image Codec uses the Java ImageIO library to encode and decode PNG images.
 */
class NativeJavaPngImageCodec(val _mathLayer : IMathLayer = JvmMathLayer)
    : IImageCodec
{
    override fun encode(data: CodecImageData): ByteArray {
        when( data.format) {
            CodecImageFormat.ARGB -> {
                // Some fairly inefficient stuff going on here.  Probably could have
                val biFormat = if( data.premultipliedAlpha) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB

                val bi = BufferedImage(data.width, data.height, biFormat)
                val internalStorage = RasterHelper.getDataStorageFromBi(bi) as IntArray

                internalStorage.forEachIndexed { index, i ->
                    val a =  data.raw[index*4 + 0]
                    val r = data.raw[index*4 + 1]
                    val g = data.raw[index*4 + 2]
                    val b = data.raw[index*4 + 3]
                    internalStorage[index] = BitPacking.packInt(a, r, g, b)
                }

                val baOut = ByteArrayOutputStream()
                ImageIO.write(bi, "png", baOut)
                return baOut.toByteArray()
            }
            else -> TODO("Not yet implemented: Format: ${data.format}")
        }
    }

    override fun decode(data: ByteArray): CodecImageData {
        val baIn = ByteArrayInputStream(data)
        val bi = ImageIO.read(baIn)

        when( bi.type)
        {
            BufferedImage.TYPE_INT_ARGB -> TODO()
            BufferedImage.TYPE_INT_ARGB_PRE -> TODO()
            BufferedImage.TYPE_4BYTE_ABGR -> {
                val internalData = RasterHelper.getDataStorageFromBi(bi) as ByteArray
                val remappedData =  ByteArray(bi.width*bi.height*4)
                for( i in remappedData.indices.step(4)) {
                    val a = internalData[i + 0]
                    val b = internalData[i + 1]
                    val g = internalData[i + 2]
                    val r = internalData[i + 3]
                    remappedData[i + 0] = a
                    remappedData[i + 1] = r
                    remappedData[i + 2] = g
                    remappedData[i + 3] = b
                }

                return CodecImageData(
                    bi.width,
                    bi.height,
                    remappedData,
                    CodecImageFormat.ARGB,
                    false )
            }
            else -> throw NotImplementedError("Unsupported BufferedImage type coming from ImageIO: ${bi.type}")
        }
    }
}