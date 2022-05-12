package rb.glow.codec

data class CodecImageData(
    val width: Int,
    val height: Int,
    val raw : ByteArray,
    val format: CodecImageFormat,
    val premultipliedAlpha : Boolean = false)

enum class CodecImageFormat {
    ARGB,
    RGB
}

interface IImageCodec : IImageEncoder, IImageDecoder

interface  IImageEncoder {
    fun encode( data: CodecImageData) : ByteArray
}

interface  IImageDecoder {
    fun decode( data: ByteArray) : CodecImageData
}