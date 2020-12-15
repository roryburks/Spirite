package rb.animo.animation

import rb.animo.DrawContract
import rb.glow.Composite
import rb.glow.IGraphicsContext
import rb.vectrix.intersect.CollisionObject
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ImmutableTransformD

data class RenderProperties(
    val trans: ITransform = ImmutableTransformD.Identity,
    val alpha: Float = 1f,
    val composite: Composite = Composite.SRC_OVER )
{
    companion object{
        val Basic = RenderProperties()
    }
}



interface IAnimation {
    val length: Double
    fun draw( gc: IGraphicsContext, met: Double) = draw(met, RenderProperties.Basic).forEach { it.drawRubrick(gc) }
    fun draw( met: Double, properties: RenderProperties, depth: Int = 0) : Collection<DrawContract>
    fun getHitbox(met: Double, type: Int) :  CollisionObject?
}

object NilAnimation : IAnimation {
    override val length: Double get() = 0.0

    override fun draw(met: Double, properties: RenderProperties, depth: Int) = emptySet<DrawContract>()
    override fun getHitbox(met: Double, type: Int) = null
}