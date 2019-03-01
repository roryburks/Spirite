package spirite.base.imageData.mediums.magLev

import spirite.base.imageData.mediums.BuiltMediumData
import spirite.pc.gui.SColor

class MaglevFill(
        val segments: List<StrokeSegment>,
        color: SColor) :
        IMaglevThing, IMaglevColorwiseThing
{
    data class StrokeSegment(
            val strokeId: Int,
            val start: Float,
            val end: Float)

    var color: SColor = color ; private set



    override fun dupe(): IMaglevThing {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun draw(built: BuiltMediumData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun transformColor(lambda: (SColor) -> SColor) {
        val oldColor = color
        val newColor = lambda(oldColor)
        if( oldColor != newColor) {
            color = newColor
        }
    }

}