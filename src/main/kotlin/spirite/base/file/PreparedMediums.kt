package spirite.base.file

import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI

/**
 * In order to save a medium using the ImageIO object, its visual data must be converted to ImageBIs (or other system
 * image format).  This should happen either right before the medium is saved or right before the medium is changed
 * (in case of multi-threaded saves), whichever comes first.
 *
 * This is a specific process, but the MediumPreparer generalizes it: when a Medium is prepared to be saved for either
 * reason, it is converted into an object which has all data needed to write the medium to the file.
 */

interface IPreparedMedium

class PreparedFlatMedium(val image: ImageBI)
    : IPreparedMedium
class PreparedDynamicMedium(
        val image: ImageBI?,
        val offsetX: Int,
        val offsetY: Int) : IPreparedMedium
class PreparedMaglevMedium(
        val things: List<IMaglevThing>) : IPreparedMedium

object MediumPreparer
{
    fun prepare( medium: IMedium) : IPreparedMedium? {
        return when( medium) {
            is FlatMedium ->  PreparedFlatMedium(Hybrid.imageConverter.convert(medium.image))
            is DynamicMedium -> PreparedDynamicMedium(
                    medium.image.base?.run { Hybrid.imageConverter.convert<ImageBI>(this) },
                    medium.image.xOffset,
                    medium.image.yOffset)
            is MaglevMedium -> PreparedMaglevMedium(medium.getThings())
            else -> null
        }
    }
}