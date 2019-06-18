package sguiSwing

import rb.glow.IImage
import rb.glow.gle.DummyConverter
import rb.glow.gle.IImageConverter

object SwProvider {
    var converter: IImageConverter = DummyConverter

    inline fun <reified T> convertOrNull(image: IImage) = converter.convertOrNull(image, T::class) as? T
}
