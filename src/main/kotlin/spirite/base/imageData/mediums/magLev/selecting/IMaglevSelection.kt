package spirite.base.imageData.mediums.magLev.selecting

import rb.glow.GraphicsContext
import rb.glow.IImage
import spirite.base.imageData.mediums.magLev.MaglevMedium

interface IMaglevLiftedData {
    val image: IImage
    fun anchorOnto( other: MaglevMedium)
}

interface IMaglevSelection {
    fun draw( gc: GraphicsContext)
    fun lift() : IMaglevLiftedData
}

