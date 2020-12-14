package spirite.base.graphics.filter

import rb.glow.Composite.CLEAR
import rb.glow.Composite.DST_IN
import rb.glow.IGraphicsContext
import rb.glow.drawer
import rb.glow.img.IImage
import rb.vectrix.mathUtil.d
import kotlin.math.min

interface IFilter {
    fun apply(gc: IGraphicsContext, w: Int, h: Int)
}

class StencilFilter(
        val image: IImage,
        val ox: Int,
        val oy: Int) : IFilter
{
    override fun apply(gc: IGraphicsContext, w: Int, h: Int) {
        gc.composite = CLEAR

        val x2 = ox + image.width
        val y2 = oy + image.height

        if( ox > 0) gc.drawer.fillRect(0.0, 0.0, min(ox, w).d, h.d)
        if( oy > 0) gc.drawer.fillRect(ox.d, 0.0, image.width.d, min(oy,h).d)
        if( x2 < w) gc.drawer.fillRect(x2.d, 0.0, w - x2.d, h.d)
        if( y2 < h) gc.drawer.fillRect(ox.d, y2.d, image.width.d, h-y2.d)

        gc.composite = DST_IN
        gc.renderImage(image, ox.d, oy.d)
    }
}