package sjunit.testHelpers.images

import rb.glow.IImage
import rb.glow.RawImage
import rb.glow.color.Color
import rb.glow.color.Colors
import spirite.base.util.linear.Rect
import kotlin.math.max

data class TRectIImage(
        val rect: Rect,
        val color: Color,
        override val width: Int = max(rect.x2, 1),
        override val height: Int = max(rect.y2, 1))
    : IImage
{
    override val byteSize: Int get() = 12
    override fun deepCopy(): RawImage { throw NotImplementedError()}

    override fun getARGB(x: Int, y: Int): Int = getColor(x,y).argb32
    override fun getColor(x: Int, y: Int): Color = if( rect.contains(x,y)) color else Colors.TRANSPARENT
}