package rb.file.imgCodec

data class CodecImageData(
    val raw : ByteArray,
    val format: RawImageFormat,
    val premultipliedAlpha : Boolean = false)

enum class RawImageFormat {
    RGBA,
    RGB
}

interface IImageCodec : IImageEncoder, IImageDecoder

interface  IImageEncoder {
    fun encode( data: CodecImageData) : ByteArray
}

interface  IImageDecoder {
    fun decode( data: ByteArray) : CodecImageData
}