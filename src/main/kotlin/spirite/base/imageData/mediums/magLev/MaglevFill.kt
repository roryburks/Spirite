package spirite.base.imageData.mediums.magLev

import rb.glow.Composite.DST_OVER
import rb.glow.Composite.SRC_OVER
import rb.vectrix.mathUtil.d
import rbJvm.glow.SColor
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.brains.toolset.MagneticFillMode.BEHIND
import spirite.base.brains.toolset.MagneticFillMode.NORMAL
import spirite.base.imageData.mediums.BuiltMediumData
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
        //val segments = segments.filter{ it.start != it.end} // Hack
        val len = segments.sumBy { abs(it.end - it.start) + 1 }
        val outX = FloatArray(len)
        val outY = FloatArray(len)
        var i = 0
        val maglev = built.arranged.handle.medium as MaglevMedium
        for( segment in segments) {
            val stroke = maglev.thingsMap[segment.strokeId] as? MaglevStroke
            if( stroke == null){
                // todo: log warning'
                println("brkp")
                continue
            }

            val stepping =
                    if(segment.end >= segment.start) segment.start..segment.end
                    else segment.start downTo segment.end

            stepping.forEach { c ->
                val nx = stroke.drawPoints.x.getOrNull(c)
                val ny = stroke.drawPoints.y.getOrNull(c)
                if( nx != null && ny != null){
                    outX[i] = stroke.drawPoints.x.getOrNull(c) ?: stroke.drawPoints.x[c-1]
                    outY[i] = stroke.drawPoints.y.getOrNull(c) ?: stroke.drawPoints.y[c-1]
                    ++i
                }
                else {
                    println("brk")
                }

            }
        }

        built.rawAccessComposite {raw ->
            val gc = raw.graphics
            gc.composite = when( mode) {
                NORMAL -> SRC_OVER
                BEHIND -> DST_OVER
            }
            gc.color = color
            gc.fillPolygon(outX.map { it.d }, outY.map { it.d }, i)
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