package sguiSwing

import rb.glow.img.IImage
import rb.glow.DummyConverter
import rb.glow.IImageConverter

object SwProvider {
    var converter: IImageConverter = DummyConverter

    inline fun <reified T> convertOrNull(image: IImage) = converter.convertOrNull(image, T::class) as? T
}
