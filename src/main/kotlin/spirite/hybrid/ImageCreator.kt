package spirite.hybrid

import spirite.base.graphics.RawImage

interface IImageCreator {
    fun createImage(width: Int, height: Int): RawImage
}