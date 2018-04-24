package spirite.base.util

import spirite.base.graphics.IImage

object ImageUtil{
    fun coordsInImage( x: Int, y: Int, image: IImage) = x >= 0 && y >= 0 && x < image.width && y < image.height
}