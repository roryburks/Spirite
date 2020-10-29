package spirite.base.file.save

import rbJvm.glow.awt.ImageBI
import sguiSwing.hybrid.Hybrid
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.actions.MaglevThingFlattener

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
        val things: List<IMaglevThing>,
        val offsetX: Int,
        val offsetY: Int,
        val image: ImageBI?) : IPreparedMedium

object MediumPreparer
{
    fun prepare( medium: IMedium) : IPreparedMedium? {
        return when( medium) {
            is FlatMedium -> PreparedFlatMedium(Hybrid.imageConverter.convert(medium.image, ImageBI::class) as ImageBI)
            is DynamicMedium -> PreparedDynamicMedium(
                    medium.image.base?.run { Hybrid.imageConverter.convert(this, ImageBI::class) as ImageBI },
                    medium.image.xOffset,
                    medium.image.yOffset)
            is MaglevMedium -> PreparedMaglevMedium(
                    MaglevThingFlattener.flattenMaglevMedium(medium),
                    medium.builtImage.xOffset,
                    medium.builtImage.yOffset,
                    medium.builtImage.base?.run { Hybrid.imageConverter.convert(this, ImageBI::class) as ImageBI })
            else -> null
        }
    }

}