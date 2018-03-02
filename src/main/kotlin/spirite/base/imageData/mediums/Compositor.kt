package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext


data class CompositeSource(
        val arranged: ArrangedMediumData,
        val drawer : (GraphicsContext) -> Unit)


class Compositor {
    var compositeSource : CompositeSource? = null
}