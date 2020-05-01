package spirite.base.imageData.mediums.magLev

import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.undo.IUndoEngine
import spirite.base.imageData.undo.ImageAction

fun IUndoEngine.performAndStoreMaglevImageAction(
        arranged: ArrangedMediumData,
        description: String,
        action: (built: BuiltMediumData, maglev: MaglevMedium) -> Any?)
{
    this.performAndStore(ConcreteMaglevImageAction(arranged, description, action))
}

class ConcreteMaglevImageAction(
        arranged: ArrangedMediumData,
        override val description: String,
        val action: (built: BuiltMediumData, maglev: MaglevMedium) -> Any?) : MaglevImageAction(arranged)
{
    override fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium) {action(built, maglev)}
}

abstract class MaglevImageAction(arrangedMediumData: ArrangedMediumData) : ImageAction(arrangedMediumData)
{
    final override fun performImageAction(built: BuiltMediumData) {
        performMaglevAction(built, built.arranged.handle.medium as MaglevMedium)
    }

    abstract fun performMaglevAction(built: BuiltMediumData, maglev: MaglevMedium)
}