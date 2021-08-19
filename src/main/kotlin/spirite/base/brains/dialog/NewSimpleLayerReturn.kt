package spirite.base.brains.dialog

import rb.glow.Color
import spirite.base.imageData.mediums.MediumType

data class NewSimpleLayerReturn(
    val width: Int,
    val height: Int,
    val color: Color,
    val name: String,
    val mediumType: MediumType
)