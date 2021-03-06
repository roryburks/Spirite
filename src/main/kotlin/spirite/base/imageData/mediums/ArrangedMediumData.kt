package spirite.base.imageData.mediums

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.selection.Selection

/***
 * ArrangedMediumData serves as a workspace upon which things are done.  Since most drawing actions and many other actions
 * depend on the various transforms and selection masks applied to the workspace in which those things are being drawn, this
 * object bakes that data and then the Medium can aggregate that into information needed to be used
 */
data class ArrangedMediumData(
        val handle: MediumHandle,
        val tMediumToWorkspace: ITransformF = ImmutableTransformF.Identity,
        val selection: Selection? = null)
{
    val built  get() = handle.medium.build(this)

    constructor(handle: MediumHandle, ox: Float, oy: Float, selection: Selection? = null ) :
            this( handle, ImmutableTransformF.Translation(ox, oy), selection)
}