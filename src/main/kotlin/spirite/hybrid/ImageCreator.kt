package spirite.hybrid

import spirite.base.graphics.NillImage
import spirite.base.graphics.RawImage
import spirite.base.graphics.gl.GLImage

interface IImageCreator {
    fun createImage(width: Int, height: Int): RawImage
}