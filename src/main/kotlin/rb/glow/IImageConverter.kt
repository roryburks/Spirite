package rb.glow

import rb.glow.img.IImage
import rb.glow.img.RawImage
import kotlin.reflect.KClass

interface IImageConverter {
    fun convertToInternal(image: IImage) : RawImage

    fun convert(image: IImage, toType : KClass<*>) : IImage
    fun convertOrNull(image: IImage, toType : KClass<*>) : IImage?
}

object DummyConverter : IImageConverter {
    override fun convertToInternal(image: IImage): RawImage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun convert(image: IImage, toType: KClass<*>): IImage {
        if( toType.isInstance(image)) return image
        throw UnsupportedOperationException("Couldn't convert image")
    }

    override fun convertOrNull(image: IImage, toType: KClass<*>): IImage? {
        return if( toType.isInstance(image)) image else null
    }

}