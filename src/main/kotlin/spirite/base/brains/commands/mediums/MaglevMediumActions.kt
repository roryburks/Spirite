package spirite.base.brains.commands.mediums

import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.magLev.MaglevMedium


object MaglevMediumActions
{
    fun convertToDynamicMedium( maglev: MaglevMedium) = DynamicMedium(maglev.workspace, maglev.builtImage.deepCopy())
}