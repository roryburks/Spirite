package rbJvm.file.imgCodec

import rb.file.imgCodec.CodecImageData
import rb.file.imgCodec.IImageCodec

/**
 * This Image Codec uses the Java ImageIO library to encode and decode PNG images.
 */
class NativeJavaPngImageCodec : IImageCodec{
    override fun encode(data: CodecImageData): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decode(data: ByteArray): CodecImageData {
        TODO("Not yet implemented")
    }
}