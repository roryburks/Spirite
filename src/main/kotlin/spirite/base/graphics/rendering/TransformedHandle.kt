package spirite.base.graphics.rendering

import rb.glow.IGraphicsContext
import rb.glow.gle.RenderMethod
import rb.glow.gle.RenderRubric
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.imageData.MediumHandle


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

    fun draw( gc: IGraphicsContext) {handle.medium.render(gc, renderRubric)}
}