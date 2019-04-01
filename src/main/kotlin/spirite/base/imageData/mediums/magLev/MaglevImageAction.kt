package spirite.base.imageData.mediums.magLev

import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.undo.ImageAction

abstract class MaglevImageAction(arrangedMediumData: ArrangedMediumData) : ImageAction(arrangedMediumData)
{
    final override fun performImageAction(built: BuiltMediumData) {
        performMaglevAction(built, built.arranged.handle.medium as MaglevMedium)
    }

    abstract fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium)
}