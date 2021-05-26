package sgui.core.systems

import rb.glow.img.RawImage

interface IImageCreator {
    fun createImage(width: Int, height: Int): RawImage
}