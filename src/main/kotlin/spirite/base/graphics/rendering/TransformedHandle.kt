package spirite.base.graphics.rendering

import spirite.base.graphics.RenderMethod
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.Transform


data class TransformedHandle(
        val handle: MediumHandle,
        val drawDepth: Int,
        val renderRubric: RenderRubric)
{
    constructor(
            handle: MediumHandle,
            depth: Int = 0,
            transform: Transform = Transform.IdentityMatrix,
            alpha: Float = 1.0f,
            renderMethod: RenderMethod? = null) : this( handle, depth, RenderRubric(transform, alpha, renderMethod))

    fun stack( other: RenderRubric) : TransformedHandle = TransformedHandle(handle, drawDepth, renderRubric.stack(other))
}
