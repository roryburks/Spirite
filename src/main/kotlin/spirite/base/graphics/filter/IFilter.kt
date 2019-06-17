package spirite.base.graphics.filter

import rb.glow.IImage
import rb.glow.Composite.CLEAR
import rb.glow.Composite.DST_IN
import rb.glow.GraphicsContext
import kotlin.math.min

interface IFilter {
    fun apply(gc: GraphicsContext, w: Int, h: Int)
}

class StencilFilter(
        val image: IImage,
        val ox: Int,
        val oy: Int) : IFilter
{
    override fun apply(gc: GraphicsContext, w: Int, h: Int) {
        gc.composite = CLEAR

        val x2 = ox + image.width
        val y2 = oy + image.height

        if( ox > 0) gc.fillRect(0, 0, min(ox, w), h)
        if( oy > 0) gc.fillRect(ox, 0, image.width, min(oy,h))
        if( x2 < w) gc.fillRect(x2, 0, w - x2, h)
        if( y2 < h) gc.fillRect(ox, y2, image.width, h-y2)

        gc.composite = DST_IN
        gc.renderImage(image, ox, oy)
    }
}