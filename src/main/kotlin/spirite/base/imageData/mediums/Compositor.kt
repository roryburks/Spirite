package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext


data class CompositeSource(
        val arranged: ArrangedMediumData,
        val drawsSource : Boolean = true,
        val drawer : (GraphicsContext) -> Unit)


class Compositor {
    var compositeSource : CompositeSource? = null
        set(value) {
            field = value
            triggerCompositeChanged()
        }

    fun triggerCompositeChanged() {
        compositeSource?.arranged?.handle?.refresh()
    }
}