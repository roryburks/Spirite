package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.MediumHandle


data class CompositeSource(
        val arranged: ArrangedMediumData,
        val drawer : (GraphicsContext) -> Unit)


class Compositor {
    var compositeSource : CompositeSource? = null
}