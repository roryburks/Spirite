package spirite.base.imageData.mediums.magLev.selecting

import rb.glow.GraphicsContext
import rb.glow.IImage
import rb.vectrix.linear.ITransformF
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.selection.ILiftedData

interface IMaglevLiftedData : ILiftedData {
    val image: IImage
    fun anchorOnto( other: MaglevMedium, tThisToOther: ITransformF)
}

interface IMaglevSelection {
    fun draw( gc: GraphicsContext)
    fun lift(arranged: ArrangedMediumData) : IMaglevLiftedData?
}

