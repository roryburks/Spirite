package spirite.base.imageData.mediums.magLev

import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.brains.toolset.MagneticFillMode.BEHIND
import spirite.base.brains.toolset.MagneticFillMode.NORMAL
import spirite.base.graphics.Composite.DST_OVER
import spirite.base.graphics.Composite.SRC_OVER
import spirite.base.imageData.mediums.BuiltMediumData
import rb.glow.color.SColor
import kotlin.math.abs

data class MaglevFill(
        val segments: List<StrokeSegment>,
        val mode: MagneticFillMode,
        var color: SColor) :
        IMaglevThing, IMaglevColorwiseThing
{
    data class StrokeSegment(
            val strokeId: Int,
            val start: Int,
            val end: Int)

    override fun dupe() = this.copy(color = color) // note: segments are currently immutable so no need to deep-copy

    override fun draw(built: BuiltMediumData) {
        val len = segments.sumBy { abs(it.end - it.start) + 1 }
        val outX = FloatArray(len)
        val outY = FloatArray(len)
        var i = 0
        val maglev = built.arranged.handle.medium as MaglevMedium
        for( segment in segments) {
            val stroke = maglev.things[segment.strokeId] as MaglevStroke

            val stepping =
                    if(segment.end >= segment.start) segment.start..segment.end
                    else segment.start downTo segment.end

            stepping.forEach { c ->
                outX[i] = stroke.drawPoints.x[c]
                outY[i] = stroke.drawPoints.y[c]
                ++i
            }
        }

        built.rawAccessComposite {raw ->
            val gc = raw.graphics
            gc.composite = when( mode) {
                NORMAL -> SRC_OVER
                BEHIND -> DST_OVER
            }
            gc.color = color
            gc.fillPolygon(outX.asList(), outY.asList(), len)
        }
    }

    override fun transformColor(lambda: (SColor) -> SColor) {
        val oldColor = color
        val newColor = lambda(oldColor)
        if( oldColor != newColor) {
            color = newColor
        }
    }
}