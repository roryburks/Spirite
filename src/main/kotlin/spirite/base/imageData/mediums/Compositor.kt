package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.MediumHandle


data class CompositeSource(
        val arranged: ArrangedMediumData,
        val drawer : (GraphicsContext) -> Unit,
        val handle: MediumHandle)


class Compositor {
    val compositeSource : CompositeSource? = null
}