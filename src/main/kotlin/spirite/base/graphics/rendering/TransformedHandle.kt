package spirite.base.graphics.rendering

import spirite.base.graphics.RenderMethod
import spirite.base.graphics.RenderRhubric
import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.MutableTransform

@Suppress("DataClassPrivateConstructor")
// There's nothing actually wrong with users accessing the default constructor; it just is a confusion to see a list when
//  the user will only ever actually put in a single entry
data class TransformedHandle
private constructor(
        val medium: MediumHandle,
        val transform: MutableTransform,
        val alpha: Float,
        val renderRhubric: RenderRhubric,
        val depth: Int)
{
    constructor(
            medium: MediumHandle,
            transform: MutableTransform = MutableTransform.IdentityMatrix(),
            alpha: Float = 1f,
            renderMethod: RenderMethod? = null,
            depth: Int = 0) : this( medium, transform, alpha, RenderRhubric(if( renderMethod == null) emptyList() else listOf(renderMethod)), depth)

    fun stack( top: TransformedHandle) : TransformedHandle {
        return TransformedHandle(
                top.medium,
                (top.transform*transform).toMutable(),
                top.alpha * alpha,
                RenderRhubric(top.renderRhubric.methods + renderRhubric.methods),
                top.depth)
    }
}