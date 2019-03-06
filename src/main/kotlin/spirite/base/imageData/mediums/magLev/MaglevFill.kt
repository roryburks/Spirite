package spirite.base.imageData.mediums.magLev

import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.util.compaction.FloatCompactor
import spirite.pc.gui.SColor
import kotlin.math.abs

class MaglevFill(
        val segments: List<StrokeSegment>,
        color: SColor) :
        IMaglevThing, IMaglevColorwiseThing
{
    data class StrokeSegment(
            val strokeId: Int,
            val start: Int,
            val end: Int)

    var color: SColor = color ; private set

    override fun dupe() = MaglevFill(
            segments,    // Currently immutable, so no need to deep-dupe
            color)

    override fun draw(built: BuiltMediumData) {
        val len = segments.sumBy { abs(it.end - it.start) + 1 }
        val outX = FloatArray(len)
        val outY = FloatArray(len)
        var i = 0
        val maglev = built.arranged.handle.medium as MaglevMedium
        for( segment in segments) {
            val stroke = maglev.things[maglev.thingMap[segment.strokeId]!!] as MaglevStroke

            val stepping =
                    if(segment.end >= segment.start) segment.start..segment.end
                    else segment.start downTo segment.end

            stepping.forEach { c ->
                outX[i] = stroke.drawPoints.x[c]
                outY[i] = stroke.drawPoints.y[c]
                ++i
            }
        }

        println(len - i)

        built.rawAccessComposite {raw ->
            val gc = raw.graphics
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