package spirite.base.graphics.rendering

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RenderMethod
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.MediumHandle
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF


data class TransformedHandle(
        val handle: MediumHandle,
        val drawDepth: Int,
        val renderRubric: RenderRubric)
{
    constructor(
            handle: MediumHandle,
            depth: Int = 0,
            transform: ITransformF = ImmutableTransformF.Identity,
            alpha: Float = 1.0f,
            renderMethod: RenderMethod? = null) : this( handle, depth, RenderRubric(transform, alpha, renderMethod))

    fun stack( other: RenderRubric) : TransformedHandle = TransformedHandle(handle, drawDepth, renderRubric.stack(other))
    fun stack( transform: ITransformF) : TransformedHandle = TransformedHandle(handle, drawDepth, renderRubric.stack(transform))

    fun draw( gc: GraphicsContext) {handle.medium.render(gc, renderRubric)}
}