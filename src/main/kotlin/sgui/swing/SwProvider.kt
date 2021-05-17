package sgui.swing

import rb.glow.DummyConverter
import rb.glow.IImageConverter
import rb.glow.img.IImage

object SwProvider {
    var converter: IImageConverter = DummyConverter

    inline fun <reified T> convertOrNull(image: IImage) = converter.convertOrNull(image, T::class) as? T
}
