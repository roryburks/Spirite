package rbJvm.glow.codec

import rb.extendo.kiebabo.BitPacking
import rb.glow.codec.CodecImageData
import rb.glow.codec.IImageCodec
import rb.glow.codec.CodecImageFormat
import rb.vectrix.IMathLayer
import rb.vectrix.mathUtil.b
import rbJvm.glow.awt.RasterHelper
import rbJvm.vectrix.JvmMathLayer
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.ComponentColorModel
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

    var colorModel : ColorModel? = null
    override fun decode(data: ByteArray): CodecImageData {
        val baIn = ByteArrayInputStream(data)
        val bi = ImageIO.read(baIn)

        when( bi.type)
        {
            BufferedImage.TYPE_INT_ARGB -> TODO()
            BufferedImage.TYPE_INT_ARGB_PRE -> TODO()
            BufferedImage.TYPE_3BYTE_BGR -> {
                val internalData = RasterHelper.getDataStorageFromBi(bi) as ByteArray
                val remappedData =  ByteArray(bi.width*bi.height*4)
                val to = bi.width * bi.height
                for( i in 0 until to) {
                    val b = internalData[i*3 + 0]
                    val g = internalData[i*3 + 1]
                    val r = internalData[i*3 + 2]
                    remappedData[i*4 + 0] = 255.b
                    remappedData[i*4 + 1] = r
                    remappedData[i*4 + 2] = g
                    remappedData[i*4 + 3] = b
                }

                return CodecImageData(
                    bi.width,
                    bi.height,
                    remappedData,
                    CodecImageFormat.ARGB,
                    false )
            }
            BufferedImage.TYPE_4BYTE_ABGR -> {
                colorModel = bi.colorModel
                val internalData = RasterHelper.getDataStorageFromBi(bi) as ByteArray
                val remappedData =  ByteArray(bi.width*bi.height*4)
                for( i in remappedData.indices.step(4)) {
                    val a = internalData[i + 0]
                    val b = internalData[i + 1]
                    val g = internalData[i + 2]
                    val r = internalData[i + 3]
                    remappedData[i + 0] = b
                    remappedData[i + 1] = g
                    remappedData[i + 2] = r
                    remappedData[i + 3] = a
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