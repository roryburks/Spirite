package rb.glow.gle

import rb.glow.IImage
import kotlin.reflect.KClass

interface IImageConverter {
    fun convert(image: IImage, toType : KClass<*>) : IImage
    fun convertOrNull(image: IImage, toType : KClass<*>) : IImage?
}

object DummyConverter : IImageConverter{
    override fun convert(image: IImage, toType: KClass<*>): IImage {
        if( toType.isInstance(image)) return image
        throw UnsupportedOperationException("Couldn't convert image")
    }

    override fun convertOrNull(image: IImage, toType: KClass<*>): IImage? {
        return if( toType.isInstance(image)) image else null
    }

}