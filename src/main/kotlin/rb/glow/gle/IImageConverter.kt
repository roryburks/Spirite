package rb.glow.gle

import rb.glow.IImage
import kotlin.reflect.KClass

interface IImageConverter {
    fun convert(image: IImage, toType : KClass<*>) : IImage
}