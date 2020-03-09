package spirite.base.imageData.mediums.magLev.selecting

import rb.glow.IImage
import rb.vectrix.linear.ITransformF
import spirite.base.exceptions.CannotDoException
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.selection.ILiftedData
import spirite.base.imageData.selection.ISelectionExtra
import spirite.base.imageData.selection.Selection

class MaglevLiftSelectionModule(
        private val arranged: ArrangedMediumData)
    : IImageDrawer.ILiftSelectionModule
{
    override fun liftSelection(selection: Selection, clearLifted: Boolean): ILiftedData? {
        val modifiedArranged = ArrangedMediumData(arranged.handle, arranged.tMediumToWorkspace, selection)
        val simpleLifted = SimpleMaglevStrokeSelectionExtra.FromArranged(modifiedArranged)

        if (!simpleLifted.lines.any()) return null
        return simpleLifted.lift(arranged)
    }

    override fun getSelectionExtra(selection: Selection): ISelectionExtra? {
        val modifiedArranged = ArrangedMediumData(arranged.handle, arranged.tMediumToWorkspace, selection)
        val simpleLifted = SimpleMaglevStrokeSelectionExtra.FromArranged(modifiedArranged)

        return if (simpleLifted.lines.any()) simpleLifted
        else null
    }
}
class MaglevAnchorLiftModule(
        private val arranged: ArrangedMediumData,
        private val maglev: MaglevMedium)
    : IImageDrawer.IAnchorLiftModule
{
    override fun acceptsLifted(lifted: ILiftedData): Boolean = lifted is IMaglevLiftedData
    override fun anchorLifted(lifted: ILiftedData, tLiftedToMedium: ITransformF?) {
        val maglevLifted = lifted as? IMaglevLiftedData ?: throw CannotDoException("Attempt to Anchor non-Maglev data onto Maglev")
        val tLiftedToMedium = tLiftedToMedium ?: ITransformF.Identity
        maglevLifted.anchorOnto(maglev, arranged, tLiftedToMedium)
    }
}