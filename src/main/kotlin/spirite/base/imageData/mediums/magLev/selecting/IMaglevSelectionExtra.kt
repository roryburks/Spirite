package spirite.base.imageData.mediums.magLev.selecting

import rb.glow.img.IImage
import rb.vectrix.linear.ITransformF
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.selection.ILiftedData
import spirite.base.imageData.selection.ISelectionExtra

interface IMaglevLiftedData : ILiftedData {
    val image: IImage
    fun anchorOnto(other: MaglevMedium, arranged: ArrangedMediumData, tMediumToLifted: ITransformF)
}

interface IMaglevSelectionExtra : ISelectionExtra{
    fun lift(arranged: ArrangedMediumData) : IMaglevLiftedData?
}

